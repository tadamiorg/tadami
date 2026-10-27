package com.sf.tadami.desktop.browser

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.sf.tadami.network.DesktopCookieJar
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.action_open_in_browser
import com.sf.tadami.shared.resources.cookies_cleared
import com.sf.tadami.shared.resources.pref_clear_cookies
import com.sf.tadami.shared.resources.refresh
import com.sf.tadami.shared.resources.stub_text
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.utils.Log
import com.sf.tadami.utils.openInBrowser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.awt.BorderLayout
import java.util.concurrent.Executors
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * The WebView of the desktop: [url] in the embedded browser, as the phone of the app's user agent, so
 * that the clearance or the login the user gets there is the OkHttp clients' one too.
 */
@Composable
fun WebViewScreen(url: String, title: String, userAgent: String?, onBack: () -> Unit) {
    var page by remember { mutableStateOf<VisiblePage?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Unpacks and starts Chromium the first time
        val started = withContext(Dispatchers.IO) {
            runCatching { EmbeddedBrowser.app() }.onFailure { Log.e(TAG, "Could not start the browser", it) }.getOrNull()
        }
        if (started == null) {
            failed = true
            return@LaunchedEffect
        }
        val agent = Injekt.get<NetworkHelper>().resolveUserAgent(userAgent)
        page = VisiblePage(started.createClient(), url, agent)
    }
    DisposableEffect(Unit) {
        onDispose { page?.close() }
    }

    val current = page
    Scaffold(
        topBar = {
            Box {
                TadaTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = current?.title?.takeIf { it.isNotBlank() } ?: title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = current?.address ?: url,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Outlined.Close, contentDescription = null)
                        }
                    },
                    // Icons only: a menu would open below the browser, which is drawn over the window
                    actions = listOf(
                        Action.Vector(
                            title = Res.string.stub_text,
                            icon = Icons.AutoMirrored.Outlined.ArrowBack,
                            onClick = { current?.browser?.goBack() },
                            enabled = current?.canGoBack == true,
                        ),
                        Action.Vector(
                            title = Res.string.stub_text,
                            icon = Icons.AutoMirrored.Outlined.ArrowForward,
                            onClick = { current?.browser?.goForward() },
                            enabled = current?.canGoForward == true,
                        ),
                        Action.Vector(
                            title = Res.string.refresh,
                            icon = Icons.Outlined.Refresh,
                            onClick = { current?.browser?.reload() },
                            enabled = current != null,
                        ),
                        Action.Vector(
                            title = Res.string.action_open_in_browser,
                            icon = Icons.Outlined.OpenInBrowser,
                            onClick = { openInBrowser(current?.address ?: url) },
                        ),
                        Action.Vector(
                            title = Res.string.pref_clear_cookies,
                            icon = Icons.Outlined.Cookie,
                            onClick = { current?.clearCookies() },
                            enabled = current != null,
                        ),
                    ),
                )
                if (current == null && !failed || current?.isLoading == true) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.BottomCenter))
                }
            }
        },
    ) { contentPadding ->
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            when {
                current != null -> SwingPanel(
                    background = Color.White,
                    factory = { current.panel },
                    modifier = Modifier.fillMaxSize(),
                )
                failed -> Text("Chromium could not start", style = MaterialTheme.typography.bodyLarge)
                else -> CircularProgressIndicator()
            }
        }
    }
}

/** A browser the user sees: its state, its cookies kept in step with the OkHttp clients'. */
private class VisiblePage(private val client: CefClient, private val url: String, userAgent: String) {

    var title by mutableStateOf<String?>(null)
    var address by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(true)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)

    /** The DevTools calls wait for the browser's thread: never made on it. */
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "WebViewScreen").apply { isDaemon = true } }

    val browser: CefBrowser

    val panel: JPanel

    init {
        client.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onAfterCreated(browser: CefBrowser) {
                // The user agent first, then the page: its first request already is the phone's
                worker.execute {
                    runCatching {
                        browser.emulate(userAgent, screen = false)
                        url.toHttpUrlOrNull()?.let { browser.pushCookies(it, DesktopCookieJar) }
                    }.onFailure { Log.e(TAG, "Could not set up the page", it) }
                    browser.loadURL(url)
                }
            }

            // Popups (ads mostly) open in the page instead of a window of their own
            override fun onBeforePopup(browser: CefBrowser, frame: CefFrame, targetUrl: String?, targetFrameName: String?): Boolean {
                if (targetUrl != null) browser.loadURL(targetUrl)
                return true
            }
        })
        client.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onTitleChange(browser: CefBrowser, title: String?) {
                this@VisiblePage.title = title
            }

            override fun onAddressChange(browser: CefBrowser, frame: CefFrame, url: String?) {
                if (frame.isMain && url != BLANK) address = url
            }
        })
        client.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadingStateChange(browser: CefBrowser, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
                this@VisiblePage.isLoading = isLoading
                this@VisiblePage.canGoBack = canGoBack
                this@VisiblePage.canGoForward = canGoForward
            }

            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain) syncCookies(frame.url)
            }
        })
        browser = client.createBrowser(BLANK, false, false)
        panel = JPanel(BorderLayout()).apply { add(browser.uiComponent, BorderLayout.CENTER) }
    }

    /** A clearance or a login obtained here is the OkHttp clients' one too. */
    private fun syncCookies(pageUrl: String?) {
        val target = pageUrl?.takeIf { it != BLANK }?.toHttpUrlOrNull() ?: return
        worker.execute { runCatching { browser.pullCookies(target, DesktopCookieJar) } }
    }

    fun clearCookies() {
        val target = (address ?: url).toHttpUrlOrNull() ?: return
        worker.execute {
            runCatching { browser.clearCookies(target, DesktopCookieJar) }
                .onSuccess { UiToasts.showToast(Res.string.cookies_cleared) }
                .onFailure { Log.e(TAG, "Could not clear the cookies", it) }
        }
    }

    fun close() {
        val last = address?.toHttpUrlOrNull()
        worker.execute {
            if (last != null) runCatching { browser.pullCookies(last, DesktopCookieJar) }
            SwingUtilities.invokeLater {
                browser.close(true)
                client.dispose()
            }
        }
        worker.shutdown()
    }
}

private const val TAG = "WebViewScreen"
private const val BLANK = "about:blank"
