package com.sf.tadami.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.network.interceptors.IgnoreGzipInterceptor
import com.sf.tadami.network.interceptors.UncaughtExceptionInterceptor
import com.sf.tadami.network.interceptors.UserAgentInterceptor
import com.sf.tadami.network.utils.UserAgents
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

/**
 * The desktop's HTTP clients, the Android ones with the app's embedded [browser] in place of the
 * WebView: [client] solves the Cloudflare challenges in it. The user agent is the settings' one, by
 * default the mobile one of the Android app: the sites serve the pages the extensions were written for.
 */
actual class NetworkHelper(cacheDir: File, browser: DesktopBrowser? = null) {

    private val dataStore: DataStore<Preferences> = Injekt.get()

    actual val cookieManager: AppCookieJar = DesktopCookieJar

    @Volatile
    private var currentAdvancedPreferences: AdvancedPreferences = runBlocking {
        dataStore.getPreferencesGroup(AdvancedPreferences)
    }

    actual val advancedPreferences: AdvancedPreferences get() = currentAdvancedPreferences

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

    actual fun resolveUserAgent(requested: String?): String =
        requested?.trim()?.takeIf { it.isNotEmpty() && it !in replacedUserAgents } ?: userAgent()

    private val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
        .cookieJar(cookieManager)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES)
        .cache(Cache(directory = File(cacheDir, "network_cache"), maxSize = 5L * 1024 * 1024))
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

    actual val client: OkHttpClient = browser?.let {
        clientBuilder.addInterceptor(it.cloudflareInterceptor(::userAgent)).build()
    } ?: nonCloudflareClient

    @Deprecated("The regular client handles Cloudflare by default")
    actual val cloudflareClient: OkHttpClient = client

    actual val webViewResolver: WebViewResolver = browser?.webViewResolver(::userAgent) ?: WebViewResolver.Unavailable()

    actual companion object {
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36"

        actual val defaultUserAgent: String
            get() = UserAgents.mobileChrome(UserAgents.FALLBACK_CHROME_MAJOR)
    }
}
