package com.sf.tadami.ui.webview

import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.sf.tadami.R
import com.sf.tadami.network.api.online.AnimeHttpSource
import com.sf.tadami.network.api.online.StubSource
import com.sf.tadami.network.requests.okhttp.HttpClient
import com.sf.tadami.network.requests.utils.WebViewUtil
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesManager
import com.sf.tadami.ui.themes.TadamiTheme
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.setComposeContent
import com.sf.tadami.utils.openInBrowser
import okhttp3.HttpUrl.Companion.toHttpUrl
import uy.kohesive.injekt.injectLazy
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class WebViewActivity : AppCompatActivity() {

    private val sourceManager: AnimeSourcesManager by injectLazy()
    private val network: HttpClient by injectLazy()

    private var assistUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.shared_axis_x_push_enter, R.anim.shared_axis_x_push_exit)
        super.onCreate(savedInstanceState)

        if (!WebViewUtil.supportsWebView(this)) {
            UiToasts.showToast(R.string.information_webview_required, Toast.LENGTH_LONG)
            finish()
            return
        }

        var url = URLDecoder.decode(intent.extras?.getString(URL_KEY), StandardCharsets.UTF_8.toString()) ?: return
        assistUrl = url

        var headers = emptyMap<String, String>()
        (sourceManager.getExtensionById(intent.extras!!.getString(SOURCE_KEY)).takeIf { it !is StubSource } as AnimeHttpSource?)?.let { source ->
            try {
                headers = source.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }
                url = source.baseUrl + url
                assistUrl = url
            } catch (e: Exception) {
                Log.e("Failed to build headers",e.stackTraceToString())
            }
        }

        setComposeContent {
            TadamiTheme {
                val systemUiController = rememberSystemUiController()
                val statusBarBackgroundColor = MaterialTheme.colorScheme.surface
                val navbarScrimColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                val isSystemInDarkTheme = isSystemInDarkTheme()

                LaunchedEffect(systemUiController, statusBarBackgroundColor) {
                    systemUiController.setStatusBarColor(
                        color = statusBarBackgroundColor,
                        darkIcons = statusBarBackgroundColor.luminance() > 0.5,
                        transformColorForLightContent = { Color.Black },
                    )
                }
                LaunchedEffect(systemUiController, isSystemInDarkTheme, navbarScrimColor) {
                    systemUiController.setNavigationBarColor(
                        color = navbarScrimColor,
                        darkIcons = !isSystemInDarkTheme,
                        navigationBarContrastEnforced = false,
                        transformColorForLightContent = { Color.Black },
                    )
                }
                WebviewScreen(
                    onNavigateUp = { finish() },
                    initialTitle = intent.extras?.getString(TITLE_KEY) ?: "",
                    url = url,
                    headers = headers,
                    onUrlChange = { assistUrl = it },
                    onOpenInBrowser = this::openInBrowser,
                    onClearCookies = this::clearCookies,
                )
            }
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        assistUrl?.let { outContent.webUri = it.toUri() }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.shared_axis_x_pop_enter, R.anim.shared_axis_x_pop_exit)
    }

    private fun openInBrowser(url: String) {
        openInBrowser(url, forceDefaultBrowser = true)
    }

    private fun clearCookies(url: String) {
        network.cookieManager.remove(url.toHttpUrl())
        Log.e("Webview","Cleared cookies for: $url")
    }

    companion object {
        private const val URL_KEY = "url_key"
        private const val SOURCE_KEY = "sourceId"
        private const val TITLE_KEY = "title_key"

        fun newIntent(context: Context, url: String, sourceId: Long? = null, title: String? = null): Intent {
            return Intent(context, WebViewActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(URL_KEY, url)
                putExtra(SOURCE_KEY, sourceId)
                putExtra(TITLE_KEY, title)
            }
        }
    }
}