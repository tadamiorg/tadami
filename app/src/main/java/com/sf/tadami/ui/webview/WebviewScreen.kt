package com.sf.tadami.ui.webview

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kevinnzou.web.AccompanistWebViewClient
import com.kevinnzou.web.LoadingState
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewNavigator
import com.kevinnzou.web.rememberWebViewState
import com.sf.tadami.BuildConfig
import com.sf.tadami.R
import com.sf.tadami.network.utils.CLOUDFLARE_CHALLENGE_PROBE
import com.sf.tadami.network.utils.applyUserAgent
import com.sf.tadami.network.utils.setDefaultSettings
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.data.DropDownAction
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

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
    val state = rememberWebViewState(url = url, additionalHttpHeaders = headers)
    val navigator = rememberWebViewNavigator()

    var currentUrl by remember { mutableStateOf(url) }
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

    val webClient = remember {
        object : AccompanistWebViewClient() {

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
                super.onPageStarted(view, url, favicon)
                url?.let {
                    currentUrl = it
                    onUrlChange(it)
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
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
                url?.let {
                    currentUrl = it
                    onUrlChange(it)
                }
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
                if (!request.isForMainFrame || navigationHeaders.isEmpty()) {
                    return false
                }
                // Continue with request, but with custom headers
                view?.loadUrl(target.toString(), navigationHeaders)
                return true
            }
        }
    }

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
                                    text = state.lastLoadedUrl ?: url,
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
                        listOf(
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
            if (challengeState != ChallengeState.NONE) {
                ChallengeBanner(
                    solved = challengeState == ChallengeState.SOLVED,
                    onDone = onNavigateUp,
                )
            }
            WebView(
                state = state,
                modifier = Modifier.fillMaxSize(),
                // Once verified, back returns to the source instead of replaying the challenge pages
                captureBackPresses = challengeState != ChallengeState.SOLVED,
                navigator = navigator,
                onCreated = { webView ->
                    webView.setDefaultSettings()

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
                    // Stop media and scripts now rather than whenever the WebView is collected
                    webView.stopLoading()
                    webView.destroy()
                },
                client = webClient,
            )
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

private enum class ChallengeState { NONE, PENDING, SOLVED }

/** Cheap check for a Cloudflare challenge page, instead of pulling the whole DOM over the bridge. */
