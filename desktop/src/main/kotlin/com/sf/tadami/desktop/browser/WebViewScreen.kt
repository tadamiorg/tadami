package com.sf.tadami.desktop.browser

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CancelPresentation
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.network.DesktopCookieJar
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.preferences.advanced.AdBlockPreferences
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.action_open_in_browser
import com.sf.tadami.shared.resources.action_webview_block_ads
import com.sf.tadami.shared.resources.action_webview_close_window
import com.sf.tadami.shared.resources.cookies_cleared
import com.sf.tadami.shared.resources.ic_vertical_settings
import com.sf.tadami.shared.resources.pref_clear_cookies
import com.sf.tadami.shared.resources.refresh
import com.sf.tadami.shared.resources.stub_text
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.data.DropDownAction
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.webview.NavigationDecision
import com.sf.tadami.ui.webview.WebViewNavigationPolicy
import com.sf.tadami.ui.webview.adblock.AdBlockRequest
import com.sf.tadami.ui.webview.adblock.RequestKind
import com.sf.tadami.utils.Log
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.openInBrowser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.cef.CefApp
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image as SkiaImage
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.awt.BorderLayout
import java.awt.CardLayout
import java.util.Base64
import java.util.concurrent.Executors
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * The WebView of the desktop, as the Android app's: [url] in the embedded browser, as the phone of the
 * app's user agent, so that the clearance or the login the user gets there is the OkHttp clients' one
 * too. Popups open as windows over the page, which the user closes; ads are blocked with the filter
 * lists of the settings.
 */
@Composable
fun WebViewScreen(url: String, title: String, userAgent: String?, onBack: () -> Unit) {
    var app by remember { mutableStateOf<CefApp?>(null) }
    var failed by remember { mutableStateOf(false) }
    // The page the screen opened, then its popups: the last one shows
    val windows = remember { mutableStateListOf<VisiblePage>() }
    // Can be turned off for this screen only: the setting applies again to the next one
    var adBlockOn by remember { mutableStateOf(true) }
    // What the toolbar tells for a moment, in place of the address: a toast would be under the browser
    var notice by remember { mutableStateOf<String?>(null) }
    // The menu would open below the browser: while it's open, a picture of the page takes its place
    var menuOpen by remember { mutableStateOf(false) }
    var pagePicture by remember { mutableStateOf<ImageBitmap?>(null) }
    // The windows: drawn by the browser, over the window, one at a time
    val container = remember { JPanel(CardLayout()) }
    val agent = remember(userAgent) { Injekt.get<NetworkHelper>().resolveUserAgent(userAgent) }
    // Whatever the lists say, the source's own site always loads
    val sourceSite = remember(url) { url.toHttpUrlOrNull()?.let { it.topPrivateDomain() ?: it.host } }

    val policy = remember {
        object : VisiblePage.Policy {
            override fun isAd(url: HttpUrl, kind: RequestKind, contextHost: String?, pageHost: String?): Boolean {
                if (!adBlockOn) return false
                return DesktopAdBlocker.isBlocked(AdBlockRequest.of(url, kind, contextHost, pageHost)) &&
                    (url.topPrivateDomain() ?: url.host) != sourceSite
            }

            override fun canOpenPopup(): Boolean = windows.size - 1 < WebViewNavigationPolicy.MAX_POPUPS

            override fun openPopup(url: String, opener: VisiblePage) {
                SwingUtilities.invokeLater {
                    val started = app ?: return@invokeLater
                    if (!canOpenPopup()) return@invokeLater
                    windows += VisiblePage(started.createClient(), url, agent, this, opener).also { container.show(it) }
                }
            }

            override fun close(page: VisiblePage) {
                SwingUtilities.invokeLater { closeWindow(windows, container, page) }
            }
        }
    }

    LaunchedEffect(Unit) {
        // Unpacks and starts Chromium the first time; the filter lists are ready before the page loads
        val started = withContext(Dispatchers.IO) {
            runCatching {
                val preferences = Injekt.get<DataStore<Preferences>>().getPreferencesGroup(AdBlockPreferences)
                adBlockOn = preferences.enabled
                if (preferences.enabled) DesktopAdBlocker.prepare(preferences)
                EmbeddedBrowser.app()
            }.onFailure { Log.e(TAG, "Could not start the browser", it) }.getOrNull()
        }
        if (started == null) {
            failed = true
            return@LaunchedEffect
        }
        app = started
        windows += VisiblePage(started.createClient(), url, agent, policy, opener = null).also { container.show(it) }
    }
    DisposableEffect(Unit) {
        onDispose { windows.forEach { it.close() } }
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(NOTICE_MILLIS)
            notice = null
        }
    }

    val current = windows.lastOrNull()
    val cookiesCleared = stringResource(Res.string.cookies_cleared)
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
                                text = notice ?: current?.address ?: url,
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
                    actions = listOfNotNull(
                        // Shown while a popup is open: back to the page below it
                        Action.Vector(
                            title = Res.string.action_webview_close_window,
                            icon = Icons.Outlined.CancelPresentation,
                            onClick = { current?.let { closeWindow(windows, container, it) } },
                        ).takeIf { windows.size > 1 },
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
                        Action.Drawable(
                            title = Res.string.stub_text,
                            icon = Res.drawable.ic_vertical_settings,
                            onClick = {
                                val page = current
                                if (page == null) {
                                    menuOpen = true
                                } else {
                                    page.snapshot { picture ->
                                        pagePicture = picture
                                        menuOpen = true
                                    }
                                }
                            },
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
                current != null -> {
                    pagePicture?.takeIf { menuOpen }?.let {
                        Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                    }
                    // Out of the window rather than out of the composition, which would close the browser;
                    // its size kept, the page doesn't lay out again
                    SwingPanel(
                        background = Color.White,
                        factory = { container },
                        modifier = Modifier.fillMaxSize().offset { if (menuOpen) IntOffset(OUT_OF_WINDOW, 0) else IntOffset.Zero },
                    )
                }
                failed -> Text("Chromium could not start", style = MaterialTheme.typography.bodyLarge)
                else -> CircularProgressIndicator()
            }
            Box(Modifier.align(Alignment.TopEnd)) {
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = {
                        menuOpen = false
                        pagePicture = null
                    },
                ) {
                    listOf(
                        DropDownAction(
                            title = stringResource(Res.string.refresh),
                            onClick = { current?.browser?.reload() },
                            enabled = current != null,
                        ),
                        DropDownAction(
                            title = stringResource(Res.string.action_open_in_browser),
                            onClick = { openInBrowser(current?.address ?: url) },
                        ),
                        DropDownAction(
                            title = stringResource(Res.string.pref_clear_cookies),
                            onClick = { current?.clearCookies { notice = cookiesCleared } },
                            enabled = current != null,
                        ),
                        DropDownAction(
                            title = stringResource(Res.string.action_webview_block_ads),
                            checked = adBlockOn,
                            onClick = {
                                adBlockOn = !adBlockOn
                                current?.browser?.reload()
                            },
                        ),
                    ).forEach { item ->
                        // As the top bar's menus
                        DropdownMenuItem(
                            onClick = {
                                item.onClick()
                                menuOpen = false
                                pagePicture = null
                            },
                            text = { Text(item.title, fontWeight = FontWeight.Normal) },
                            enabled = item.enabled,
                            trailingIcon = item.checked?.let { checked ->
                                { Checkbox(checked = checked, onCheckedChange = null) }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun JPanel.show(page: VisiblePage) {
    add(page.panel, page.id)
    (layout as CardLayout).show(this, page.id)
    revalidate()
}

/** Closes a popup, the page below it shows again; never the page the screen opened. */
private fun closeWindow(windows: MutableList<VisiblePage>, container: JPanel, page: VisiblePage) {
    if (page === windows.firstOrNull() || !windows.remove(page)) return
    container.remove(page.panel)
    windows.lastOrNull()?.let { (container.layout as CardLayout).show(container, it.id) }
    container.revalidate()
    page.close()
}

/** A browser the user sees: its state, its cookies kept in step with the OkHttp clients'. */
private class VisiblePage(
    private val client: CefClient,
    private val url: String,
    userAgent: String,
    private val policy: Policy,
    private val opener: VisiblePage?,
) {

    /** What the screen decides for the page: the ads, the popups. Called from the browser's threads. */
    interface Policy {
        fun isAd(url: HttpUrl, kind: RequestKind, contextHost: String?, pageHost: String? = null): Boolean
        fun canOpenPopup(): Boolean
        fun openPopup(url: String, opener: VisiblePage)
        fun close(page: VisiblePage)
    }

    val id = "page-${System.identityHashCode(this)}"

    var title by mutableStateOf<String?>(null)
    var address by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(true)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)

    private val isPopup get() = opener != null

    @Volatile
    private var pageHost: String? = null

    @Volatile
    private var initialLoadDone = false

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

            // A window of its own, over this one, unless it's an ad
            override fun onBeforePopup(browser: CefBrowser, frame: CefFrame, targetUrl: String?, targetFrameName: String?): Boolean {
                val target = targetUrl?.toHttpUrlOrNull() ?: return true
                val contextHost = if (isPopup) opener?.pageHost else pageHost
                if (policy.canOpenPopup() && !policy.isAd(target, RequestKind.POPUP, contextHost)) {
                    policy.openPopup(target.toString(), this@VisiblePage)
                }
                return true
            }
        })
        client.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onTitleChange(browser: CefBrowser, title: String?) {
                this@VisiblePage.title = title
            }

            override fun onAddressChange(browser: CefBrowser, frame: CefFrame, url: String?) {
                if (!frame.isMain || url == BLANK) return
                address = url
                pageHost = url?.toHttpUrlOrNull()?.host
            }
        })
        client.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadingStateChange(browser: CefBrowser, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
                this@VisiblePage.isLoading = isLoading
                this@VisiblePage.canGoBack = canGoBack
                this@VisiblePage.canGoForward = canGoForward
            }

            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (!frame.isMain || frame.url == BLANK) return
                initialLoadDone = true
                syncCookies(frame.url)
            }
        })
        val resourceHandler = object : CefResourceRequestHandlerAdapter() {
            // What the page loads, without its ads
            override fun onBeforeResourceLoad(browser: CefBrowser, frame: CefFrame?, request: CefRequest): Boolean {
                if (request.resourceType == CefRequest.ResourceType.RT_MAIN_FRAME) return false
                val target = request.url?.toHttpUrlOrNull() ?: return false
                val frameHost = frame?.url?.toHttpUrlOrNull()?.host
                return policy.isAd(target, RequestKind.SUBRESOURCE, frameHost ?: pageHost, pageHost)
            }
        }
        client.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(
                browser: CefBrowser,
                frame: CefFrame,
                request: CefRequest,
                userGesture: Boolean,
                isRedirect: Boolean,
            ): Boolean {
                val target = request.url ?: return true
                val scheme = target.substringBefore(':').lowercase()
                if (scheme in LOCAL_SCHEMES) return false
                // intent://, market://, mailto:... nothing the browser opens
                if (scheme != "http" && scheme != "https") return true
                if (!frame.isMain) return false
                val decision = WebViewNavigationPolicy.decide(
                    currentUrl = address,
                    targetUrl = target,
                    isRedirect = isRedirect,
                    initialLoadDone = initialLoadDone,
                    isPopup = isPopup,
                    canOpenPopup = policy.canOpenPopup(),
                    isAd = { url, asPopup ->
                        // A popup's pages, like a page opening in a new window, against the page that opened it
                        val contextHost = if (isPopup) opener?.pageHost else pageHost
                        policy.isAd(url, if (asPopup) RequestKind.POPUP else RequestKind.DOCUMENT, contextHost)
                    },
                )
                return when (decision) {
                    NavigationDecision.ALLOW -> false
                    // Another site taking over the page (an ad script setting top.location): the page stays
                    NavigationDecision.POPUP -> {
                        policy.openPopup(target, this@VisiblePage)
                        true
                    }
                    NavigationDecision.BLOCK -> {
                        // An ad chain: the popup has nothing else to show
                        if (isPopup && (!initialLoadDone || isRedirect)) policy.close(this@VisiblePage)
                        true
                    }
                }
            }

            override fun getResourceRequestHandler(
                browser: CefBrowser,
                frame: CefFrame?,
                request: CefRequest,
                isNavigation: Boolean,
                isDownload: Boolean,
                requestInitiator: String?,
                disableDefaultHandling: BoolRef,
            ): CefResourceRequestHandler = resourceHandler
        })
        browser = client.createBrowser(BLANK, false, false)
        panel = JPanel(BorderLayout()).apply { add(browser.uiComponent, BorderLayout.CENTER) }
    }

    /** A clearance or a login obtained here is the OkHttp clients' one too. */
    private fun syncCookies(pageUrl: String?) {
        val target = pageUrl?.takeIf { it != BLANK }?.toHttpUrlOrNull() ?: return
        worker.execute { runCatching { browser.pullCookies(target, DesktopCookieJar) } }
    }

    /** Forgets the cookies of the page, then loads it again without them. */
    /** The page as it shows, on the UI thread: null when the browser can't draw it. */
    fun snapshot(onTaken: (ImageBitmap?) -> Unit) {
        worker.execute {
            val picture = runCatching {
                val data = browser.devTools("Page.captureScreenshot", buildJsonObject { put("format", "png") })
                    .getValue("data").jsonPrimitive.content
                SkiaImage.makeFromEncoded(Base64.getDecoder().decode(data)).toComposeImageBitmap()
            }.onFailure { Log.d(TAG, "No picture of the page: ${it.message}") }.getOrNull()
            SwingUtilities.invokeLater { onTaken(picture) }
        }
    }

    fun clearCookies(onCleared: () -> Unit) {
        val target = (address ?: url).toHttpUrlOrNull() ?: return
        worker.execute {
            runCatching { browser.clearCookies(target, DesktopCookieJar) }
                .onSuccess {
                    onCleared()
                    browser.reload()
                }
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
private const val NOTICE_MILLIS = 3000L
// Far enough for any screen
private const val OUT_OF_WINDOW = 100_000
private val LOCAL_SCHEMES = setOf("about", "blob", "data", "devtools", "chrome")
