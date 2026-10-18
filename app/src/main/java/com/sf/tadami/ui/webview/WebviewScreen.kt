package com.sf.tadami.ui.webview

import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CancelPresentation
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kevinnzou.web.AccompanistWebChromeClient
import com.kevinnzou.web.AccompanistWebViewClient
import com.kevinnzou.web.LoadingState
import com.kevinnzou.web.WebContent
import com.kevinnzou.web.WebView
import com.kevinnzou.web.WebViewNavigator
import com.kevinnzou.web.WebViewState
import com.sf.tadami.BuildConfig
import com.sf.tadami.R
import com.sf.tadami.network.utils.CLOUDFLARE_CHALLENGE_PROBE
import com.sf.tadami.network.utils.applyUserAgent
import com.sf.tadami.network.utils.setDefaultSettings
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.data.DropDownAction
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.utils.findActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

/**
 * A page of the screen: the one it opened, or a popup (`window.open`, a `target=_blank` link) stacked
 * above it. Each window has its own clients, so a page loading under a popup doesn't change the header.
 */
private class WebViewWindow(
    content: WebContent,
    val navigator: WebViewNavigator,
    initialUrl: String,
    // The popup's WebView goes back to the page that opened it through this message
    val popupMessage: Message? = null,
) {
    val state = WebViewState(content)
    var currentUrl by mutableStateOf(initialUrl)
    var webView: WebView? = null
    // Until the page the window opened finished loading, its navigations are its redirects
    var initialLoadDone = false
    // A popup opened on an ad closes, it has nothing else to show
    var pageStarted = false
    lateinit var client: AccompanistWebViewClient
    lateinit var chromeClient: AccompanistWebChromeClient
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebviewScreen(
    onNavigateUp: () -> Unit,
    initialTitle: String,
    url: String,
    userAgent: String,
    onOpenInBrowser: (String) -> Unit,
    onClearCookies: (String) -> Unit,
    hasClearance: (String) -> Boolean,
    headers: Map<String, String> = emptyMap(),
    onUrlChange: (String) -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    var challengeState by remember { mutableStateOf(ChallengeState.NONE) }
    var solvedUrl by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { WebViewAdBlocker.load(context.applicationContext) }
    }

    // The user agent travels through the WebView settings, only re-send the other source headers
    val navigationHeaders = remember(headers) {
        headers.filterKeys { !it.equals("user-agent", ignoreCase = true) }
    }
    // The source's headers are for the source: the host of the page the screen opened
    val headersHost = remember(url) { url.toUri().host }

    val windows = remember { mutableStateListOf<WebViewWindow>() }
    // Destroyed once: by the window closing, or by the screen leaving
    val destroyedWebViews = remember { mutableSetOf<WebView>() }
    var screenActive by remember { mutableStateOf(true) }
    var rendererGoneReported by remember { mutableStateOf(false) }

    // A web video player in fullscreen (HTML5 <video> or an embedded player), shown over the screen
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    fun exitFullscreen() {
        val callback = fullscreenCallback ?: return
        fullscreenView = null
        fullscreenCallback = null
        // Tells the page, which leaves its fullscreen
        callback.onCustomViewHidden()
    }

    fun destroy(webView: WebView) {
        if (!destroyedWebViews.add(webView)) return
        // Stop media and scripts now rather than whenever the WebView is collected
        webView.stopLoading()
        webView.destroy()
    }

    fun closeWindow(window: WebViewWindow) {
        if (window === windows.first()) return
        val visible = window === windows.last()
        if (visible) exitFullscreen()
        if (!windows.remove(window)) return
        // The visible one is destroyed when it leaves the composition, the ones below it now
        if (!visible) window.webView?.let(::destroy)
    }

    fun onRendererGone(window: WebViewWindow) {
        if (!rendererGoneReported) {
            rendererGoneReported = true
            UiToasts.showToast(R.string.information_webview_crashed, Toast.LENGTH_LONG)
        }
        // Its WebView is no use anymore: a popup closes, the page the screen opened closes the screen
        if (window === windows.firstOrNull()) onNavigateUp() else closeWindow(window)
    }

    fun createWebClient(window: WebViewWindow, openPopup: (String) -> Unit) =
        object : AccompanistWebViewClient() {

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                // Each WebView using the renderer hears about it. Not handling it kills the app
                Log.e(
                    TAG,
                    "WebView renderer gone: crashed=${detail.didCrash()}, " +
                        "priority=${detail.rendererPriorityAtExit()}, url=${window.currentUrl}",
                )
                onRendererGone(window)
                return true
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                // Never block the page itself, only what it loads
                if (request != null && !request.isForMainFrame && WebViewAdBlocker.isBlocked(request.url.host)) {
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                // An ad in a popup, the one shouldOverrideUrlLoading didn't see
                if (window !== windows.firstOrNull() && WebViewAdBlocker.isBlocked(url?.toUri()?.host)) {
                    if (window.pageStarted) view.stopLoading() else view.post { closeWindow(window) }
                    return
                }
                window.pageStarted = true
                super.onPageStarted(view, url, favicon)
                url?.let { window.currentUrl = it }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                window.initialLoadDone = true
                // The challenges are solved in the page the screen opened
                if (window !== windows.firstOrNull()) return
                val pageUrl = url ?: return
                view.evaluateJavascript(CLOUDFLARE_CHALLENGE_PROBE) { result ->
                    challengeState = when {
                        result == "true" -> ChallengeState.PENDING
                        challengeState == ChallengeState.PENDING && hasClearance(pageUrl) -> {
                            solvedUrl = pageUrl
                            ChallengeState.SOLVED
                        }
                        // Only on the verified page: browsing on gives the back button its history back
                        challengeState == ChallengeState.SOLVED && pageUrl == solvedUrl -> ChallengeState.SOLVED
                        else -> ChallengeState.NONE
                    }
                }
            }

            override fun doUpdateVisitedHistory(
                view: WebView,
                url: String?,
                isReload: Boolean,
            ) {
                super.doUpdateVisitedHistory(view, url, isReload)
                url?.let { window.currentUrl = it }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val target = request?.url ?: return false
                val scheme = target.scheme?.lowercase()
                // Don't attempt to open blobs as webpages
                if (scheme == "blob" || scheme == "data" || scheme == "about") {
                    return false
                }
                // Ignore intent://, market://, mailto:... the WebView can't open them
                if (scheme != "http" && scheme != "https") {
                    return true
                }
                if (!request.isForMainFrame) {
                    return false
                }
                val decision = WebViewNavigationPolicy.decide(
                    currentUrl = view?.url,
                    targetUrl = target.toString(),
                    isRedirect = request.isRedirect,
                    initialLoadDone = window.initialLoadDone,
                    isPopup = window !== windows.firstOrNull(),
                    canOpenPopup = windows.size - 1 < WebViewNavigationPolicy.MAX_POPUPS,
                    isAdHost = WebViewAdBlocker::isBlocked,
                )
                when (decision) {
                    NavigationDecision.ALLOW -> {}
                    NavigationDecision.POPUP -> {
                        // Another site taking over the page (an ad script setting top.location): the page stays
                        openPopup(target.toString())
                        return true
                    }
                    NavigationDecision.BLOCK -> {
                        if (window !== windows.firstOrNull() && !window.pageStarted) {
                            view?.post { closeWindow(window) }
                        }
                        return true
                    }
                }
                if (navigationHeaders.isEmpty() ||
                    !target.host.equals(headersHost, ignoreCase = true) ||
                    // The page shown, like a reload: the WebView handles it
                    target.toString() == view?.url
                ) {
                    return false
                }
                // Continue with request, but with custom headers
                view?.loadUrl(target.toString(), navigationHeaders)
                return true
            }
        }

    fun createWindow(content: WebContent, initialUrl: String, popupMessage: Message? = null): WebViewWindow =
        WebViewWindow(content, WebViewNavigator(coroutineScope), initialUrl, popupMessage).also { window ->
            window.client = createWebClient(window) { popupUrl ->
                windows.add(createWindow(WebContent.Url(url = popupUrl), popupUrl))
            }
            window.chromeClient = object : AccompanistWebChromeClient() {
                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message,
                ): Boolean {
                    // Like a browser's popup blocker: only the ones the user asked for, and not endlessly
                    if (!isUserGesture || windows.size - 1 >= WebViewNavigationPolicy.MAX_POPUPS) return false
                    windows.add(createWindow(WebContent.NavigatorOnly, "", resultMsg))
                    return true
                }

                override fun onCloseWindow(view: WebView) {
                    // window.close()
                    windows.find { it.webView === view }?.let(::closeWindow)
                }

                override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
                    // One at a time
                    if (fullscreenView != null) {
                        callback.onCustomViewHidden()
                        return
                    }
                    fullscreenView = view
                    fullscreenCallback = callback
                }

                override fun onHideCustomView() {
                    // The player's own exit button
                    fullscreenView = null
                    fullscreenCallback = null
                }
            }
        }

    remember {
        windows.add(createWindow(WebContent.Url(url = url, additionalHttpHeaders = headers), url))
    }
    val rootWindow = windows.first()
    val currentWindow = windows.last()
    val state = currentWindow.state
    val navigator = currentWindow.navigator
    val currentUrl = currentWindow.currentUrl

    LaunchedEffect(currentUrl) {
        onUrlChange(currentUrl)
    }

    DisposableEffect(Unit) {
        onDispose {
            screenActive = false
            exitFullscreen()
            windows.forEach { window -> window.webView?.let(::destroy) }
        }
    }

    // Back closes the popup once its own history is done (the WebView handles that first)
    BackHandler(enabled = windows.size > 1) {
        closeWindow(currentWindow)
    }

    Box {
        Scaffold(
            topBar = {
                Box {
                    Column {
                        TadaTopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = state.pageTitle ?: initialTitle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = state.lastLoadedUrl ?: currentUrl,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onNavigateUp) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = null,
                                    )
                                }
                            },
                            actions =
                            listOfNotNull(
                                // Shown while a popup is open: back to the page below it
                                Action.Vector(
                                    title = R.string.action_webview_close_window,
                                    icon = Icons.Outlined.CancelPresentation,
                                    onClick = {
                                        closeWindow(currentWindow)
                                    },
                                ).takeIf { windows.size > 1 },
                                Action.Vector(
                                    title = R.string.stub_text,
                                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                    onClick = {
                                        if (navigator.canGoBack) {
                                            navigator.navigateBack()
                                        }
                                    },
                                    enabled = navigator.canGoBack,
                                ),
                                Action.Vector(
                                    title = R.string.stub_text,
                                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                                    onClick = {
                                        if (navigator.canGoForward) {
                                            navigator.navigateForward()
                                        }
                                    },
                                    enabled = navigator.canGoForward,
                                ),
                                Action.DropDownDrawable(
                                    title = R.string.stub_text,
                                    icon = R.drawable.ic_vertical_settings,
                                    items =
                                    listOf(
                                        DropDownAction(
                                            title = stringResource(id = R.string.refresh),
                                            onClick = {
                                                navigator.reload()
                                            }
                                        ),
                                        DropDownAction(
                                            title = stringResource(id = R.string.action_open_in_browser),
                                            onClick = {
                                                onOpenInBrowser(currentUrl)
                                            }
                                        ),
                                        DropDownAction(
                                            title = stringResource(id = R.string.pref_clear_cookies),
                                            onClick = {
                                                onClearCookies(currentUrl)
                                            }
                                        )
                                    )
                                )
                            )
                        )
                    }
                    when (val loadingState = state.loadingState) {
                        is LoadingState.Initializing -> LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                        )

                        is LoadingState.Loading -> LinearProgressIndicator(
                            progress = { loadingState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                        )

                        else -> {}
                    }
                }
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                if (challengeState != ChallengeState.NONE && currentWindow === rootWindow) {
                    ChallengeBanner(
                        solved = challengeState == ChallengeState.SOLVED,
                        onDone = onNavigateUp,
                    )
                }
                // Keyed to the window: showing another window creates the composable again with its WebView
                key(currentWindow) {
                    WebView(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                        // Once verified, back returns to the source instead of replaying the challenge pages
                        captureBackPresses = currentWindow !== rootWindow || challengeState != ChallengeState.SOLVED,
                        navigator = navigator,
                        onCreated = { webView ->
                            webView.setDefaultSettings()
                            // Popups open in a window of their own, see onCreateWindow
                            webView.settings.setSupportMultipleWindows(true)
                            webView.settings.javaScriptCanOpenWindowsAutomatically = true

                            // Debug mode (chrome://inspect/#devices)
                            if (BuildConfig.DEBUG &&
                                0 != webView.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
                            ) {
                                WebView.setWebContentsDebuggingEnabled(true)
                            }

                            // Same UA as OkHttp, or the clearance obtained here would be useless to it
                            webView.applyUserAgent(userAgent)
                        },
                        onDispose = { webView ->
                            val window = windows.find { it.webView === webView }
                            if (window == null || !screenActive) {
                                destroy(webView)
                            } else {
                                // Covered by a popup: kept as it is, not loaded again when shown again
                                window.state.content = WebContent.NavigatorOnly
                            }
                        },
                        client = currentWindow.client,
                        chromeClient = currentWindow.chromeClient,
                        factory = { context ->
                            currentWindow.webView ?: WebView(context).also { webView ->
                                currentWindow.webView = webView
                                currentWindow.popupMessage?.let { message ->
                                    (message.obj as WebView.WebViewTransport).webView = webView
                                    message.sendToTarget()
                                }
                            }
                        },
                    )
                }
            }
        }

        val fullscreen = fullscreenView
        if (fullscreen != null) {
            key(fullscreen) {
                FullscreenView(view = fullscreen, onExit = ::exitFullscreen)
            }
        }
    }
}

@Composable
private fun ChallengeBanner(solved: Boolean, onDone: () -> Unit) {
    Surface(
        color = if (solved) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(if (solved) R.string.webview_challenge_solved else R.string.webview_challenge_pending),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (solved) {
                TextButton(onClick = onDone) {
                    Text(text = stringResource(R.string.webview_challenge_done))
                }
            }
        }
    }
}

/**
 * [view] over the whole screen on a black background, the system bars hidden (a swipe shows them for
 * a moment) and any orientation allowed. Leaving it restores the bars and the orientation.
 */
@Composable
private fun FullscreenView(view: View, onExit: () -> Unit) {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(view) {
        val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        val previousBehavior = insetsController.systemBarsBehavior
        val previousOrientation = activity.requestedOrientation
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = previousBehavior
            activity.requestedOrientation = previousOrientation
        }
    }

    // After the WebView's own back handling: back leaves the fullscreen first
    BackHandler(onBack = onExit)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { view },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private enum class ChallengeState { NONE, PENDING, SOLVED }

private const val TAG = "WebviewScreen"

/** Cheap check for a Cloudflare challenge page, instead of pulling the whole DOM over the bridge. */
