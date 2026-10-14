package com.sf.tadami.ui.webview

import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.luminance
import androidx.core.net.toUri
import com.sf.tadami.R
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.utils.WebViewUtil
import com.sf.tadami.source.online.AnimeHttpSource
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.setComposeContent
import com.sf.tadami.utils.openInBrowser
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import uy.kohesive.injekt.injectLazy
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class WebViewActivity : AppCompatActivity() {

    private val sourceManager: SourceManager by injectLazy()
    private val network: NetworkHelper by injectLazy()

    private var assistUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.shared_axis_x_push_enter, R.anim.shared_axis_x_push_exit)
        super.onCreate(savedInstanceState)

        if (!WebViewUtil.supportsWebView(this)) {
            UiToasts.showToast(R.string.information_webview_required, Toast.LENGTH_LONG)
            finish()
            return
        }

        val url = intent.extras?.getString(URL_KEY)?.let(::decodeExtra)
        if (url == null) {
            finish()
            return
        }
        assistUrl = url

        var headers = emptyMap<String, String>()
        (sourceManager.get(intent.extras!!.getLong(SOURCE_KEY)) as? AnimeHttpSource)?.let { source ->
            try {
                headers = source.headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }
            } catch (e: Exception) {
                Log.d("Failed to build headers", e.stackTraceToString())
            }
        }
        // Must match what OkHttp sends: Cloudflare binds its clearance to the user agent
        val userAgent = network.resolveUserAgent(headers["user-agent"])

        setComposeContent {
            val statusBarBackgroundColor = MaterialTheme.colorScheme.surface
            val isSystemInDarkTheme = isSystemInDarkTheme()

            LaunchedEffect(isSystemInDarkTheme, statusBarBackgroundColor) {
                // Draw edge-to-edge and set system bars color to transparent
                val lightStyle = SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.BLACK
                )
                val darkStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(
                    statusBarStyle = if (statusBarBackgroundColor.luminance() > 0.5) lightStyle else darkStyle,
                    navigationBarStyle = if (isSystemInDarkTheme) darkStyle else lightStyle,
                )
            }
            WebviewScreen(
                onNavigateUp = { finish() },
                initialTitle = decodeExtra(intent.extras?.getString(TITLE_KEY) ?: ""),
                url = url,
                userAgent = userAgent,
                headers = headers,
                onUrlChange = { assistUrl = it },
                onOpenInBrowser = this::openInBrowser,
                onClearCookies = this::clearCookies,
                hasClearance = this::hasClearance,
            )
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        assistUrl?.let { outContent.webUri = it.toUri() }
    }

    override fun onPause() {
        super.onPause()
        // A clearance solved here must survive the app being killed right after
        network.cookieManager.flush()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.shared_axis_x_pop_enter, R.anim.shared_axis_x_pop_exit)
    }

    private fun openInBrowser(url: String) {
        openInBrowser(url, forceDefaultBrowser = true)
    }

    private fun clearCookies(url: String) {
        val httpUrl = url.toHttpUrlOrNull() ?: return
        val cleared = network.cookieManager.remove(httpUrl)
        Log.i("Webview", "Cleared $cleared cookies for: $url")
    }

    private fun hasClearance(url: String): Boolean = network.cookieManager.hasCloudflareClearance(url)

    /**
     * Extras may arrive URL-encoded (player) or already decoded by Navigation (details screen);
     * a raw '%' must not crash the screen.
     */
    private fun decodeExtra(value: String): String {
        return try {
            URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
        } catch (e: IllegalArgumentException) {
            value
        }
    }

    companion object {
        private const val URL_KEY = "url_key"
        private const val SOURCE_KEY = "sourceId"
        private const val TITLE_KEY = "title_key"

        fun newIntent(
            context: Context,
            url: String,
            sourceId: Long? = null,
            title: String? = null
        ): Intent {
            return Intent(context, WebViewActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(URL_KEY, url)
                putExtra(SOURCE_KEY, sourceId)
                putExtra(TITLE_KEY, title)
            }
        }
    }
}