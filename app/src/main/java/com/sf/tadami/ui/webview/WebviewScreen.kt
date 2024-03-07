package com.sf.tadami.ui.webview

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.MaterialTheme
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import com.sf.tadami.BuildConfig
import com.sf.tadami.R
import com.sf.tadami.network.utils.setDefaultSettings
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.data.DropDownAction
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebviewScreen(
    onNavigateUp: () -> Unit,
    initialTitle: String,
    url: String,
    onOpenInBrowser: (String) -> Unit,
    onClearCookies: (String) -> Unit,
    headers: Map<String, String> = emptyMap(),
    onUrlChange: (String) -> Unit = {},
) {
    val state = rememberWebViewState(url = url, additionalHttpHeaders = headers)
    val navigator = rememberWebViewNavigator()

    var currentUrl by remember { mutableStateOf(url) }
    val context = LocalContext.current

    val webClient = remember {
        object : AccompanistWebViewClient() {
            private lateinit var adservers : StringBuilder
            init {
                readAdServers()
            }

            private fun readAdServers()
            {
                adservers = StringBuilder()

                var line: String?
                val inputStream = context.resources.openRawResource(R.raw.adservers)
                val br = BufferedReader(InputStreamReader(inputStream))

                try
                {
                    while (br.readLine().also { line = it } != null)
                    {
                        adservers.append(line)
                        adservers.append("\n")
                    }
                }
                catch (e: IOException)
                {
                    e.printStackTrace()
                }
            }
            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let {
                    currentUrl = it
                    onUrlChange(it)
                }
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val empty = ByteArrayInputStream("".toByteArray())
                val kk5 = adservers.toString()

                if (request != null) {
                    if (kk5.contains("0.0.0.0 " + request.url.host)){
                        return WebResourceResponse("text/plain", "utf-8", empty)
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
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
                request?.let {
                    // Don't attempt to open blobs as webpages
                    if (it.url.toString().startsWith("blob:http")) {
                        return false
                    }

                    // Continue with request, but with custom headers
                    view?.loadUrl(it.url.toString(), headers)
                }
                return super.shouldOverrideUrlLoading(view, request)
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
                        progress = { (loadingState as? LoadingState.Loading)?.progress ?: 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                    )

                    else -> {}
                }
            }
        },
    ) { contentPadding ->
        WebView(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            navigator = navigator,
            onCreated = { webView ->
                webView.setDefaultSettings()

                // Debug mode (chrome://inspect/#devices)
                if (BuildConfig.DEBUG &&
                    0 != webView.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
                ) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }

                headers["user-agent"]?.let {
                    webView.settings.userAgentString = it
                }
            },
            client = webClient,
        )
    }
}