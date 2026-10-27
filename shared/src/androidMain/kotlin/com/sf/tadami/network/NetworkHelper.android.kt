package com.sf.tadami.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.network.interceptors.CloudflareInterceptor
import com.sf.tadami.network.interceptors.IgnoreGzipInterceptor
import com.sf.tadami.network.interceptors.UncaughtExceptionInterceptor
import com.sf.tadami.network.interceptors.UserAgentInterceptor
import com.sf.tadami.network.utils.WebViewUtil
import com.sf.tadami.preferences.advanced.AdvancedPreferences
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

actual class NetworkHelper(private val context : Context) {

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    private val androidCookieJar = AndroidCookieJar()

    actual val cookieManager: AppCookieJar = androidCookieJar

    private val dataStore : DataStore<Preferences> = Injekt.get()

    /**
     * Read once synchronously, so the first requests already send the user's user agent: Cloudflare
     * ties its clearance to it. Then follows the settings, so a change applies without a restart.
     */
    @Volatile
    private var currentAdvancedPreferences: AdvancedPreferences = runBlocking {
        dataStore.getPreferencesGroup(AdvancedPreferences)
    }

    actual val advancedPreferences: AdvancedPreferences get() = currentAdvancedPreferences

    /** User agents the settings replaced, still in the headers of the sources built before */
    private val replacedUserAgents: MutableSet<String> = ConcurrentHashMap.newKeySet()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            dataStore.getPreferencesGroupAsFlow(AdvancedPreferences).collect { preferences ->
                val previous = userAgent()
                currentAdvancedPreferences = preferences
                val current = userAgent()
                if (previous != current) {
                    replacedUserAgents += previous
                    replacedUserAgents -= current
                }
            }
        }
    }

    private fun userAgent() = advancedPreferences.userAgent.trim()

    /** The user agent to send for a request asking for [requested]: the current one instead of a replaced one. */
    actual fun resolveUserAgent(requested: String?): String =
        requested?.trim()?.takeIf { it.isNotEmpty() && it !in replacedUserAgents } ?: userAgent()

    private val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
        .cookieJar(androidCookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES)
        .cache(
            Cache(
                directory = cacheDir,
                maxSize = cacheSize, // 5 MiB
            ),
        )
        .addInterceptor(UncaughtExceptionInterceptor())
        .addInterceptor(
            UserAgentInterceptor(
                userAgent = ::userAgent,
                replaces = { it.trim() in replacedUserAgents },
            ),
        )
        .addNetworkInterceptor(IgnoreGzipInterceptor())
        .addNetworkInterceptor(BrotliInterceptor)

    actual val nonCloudflareClient: OkHttpClient = clientBuilder.build()

    actual val client: OkHttpClient = clientBuilder
        .addInterceptor(
            CloudflareInterceptor(context, androidCookieJar, ::userAgent),
        )
        .build()

    /**
     * @deprecated Since Tadami 1.5.0
     */
    @Deprecated("The regular client handles Cloudflare by default")
    @Suppress("UNUSED")
    actual val cloudflareClient: OkHttpClient = client

    /**
     * Headless WebView sharing this client's cookies and user agent, for pages that need
     * JavaScript to produce their content.
     *
     * @since extensions-lib 1.6
     */
    actual val webViewResolver: WebViewResolver by lazy {
        AndroidWebViewResolver(context, androidCookieJar, ::userAgent)
    }

    actual companion object{
        /**
         * Last-resort default, only used when the WebView version can't be read. Extensions
         * shouldn't set a user agent at all: every request already gets the app's one.
         */
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36"

        /** Default user agent, following the installed WebView's Chrome version. */
        actual val defaultUserAgent: String
            get() = WebViewUtil.defaultUserAgent
    }
}
