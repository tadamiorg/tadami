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
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.TimeUnit

class NetworkHelper(private val context : Context) {

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar()

    private val dataStore : DataStore<Preferences> = Injekt.get()

    val advancedPreferences = runBlocking {
        dataStore.getPreferencesGroup(AdvancedPreferences)
    }

    private val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
        .cookieJar(cookieManager)
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
        .addInterceptor(UserAgentInterceptor(advancedPreferences.userAgent.trim()))
        .addNetworkInterceptor(IgnoreGzipInterceptor())
        .addNetworkInterceptor(BrotliInterceptor)

    val nonCloudflareClient = clientBuilder.build()

    val client = clientBuilder
        .addInterceptor(
            CloudflareInterceptor(context, cookieManager, advancedPreferences.userAgent.trim()),
        )
        .build()

    /**
     * @deprecated Since Tadami 1.5.0
     */
    @Deprecated("The regular client handles Cloudflare by default")
    @Suppress("UNUSED")
    val cloudflareClient: OkHttpClient = client

    /**
     * Headless WebView sharing this client's cookies and user agent, for pages that need
     * JavaScript to produce their content.
     *
     * @since extensions-lib 1.6
     */
    val webViewResolver: WebViewResolver by lazy {
        WebViewResolver(context, cookieManager) { advancedPreferences.userAgent.trim() }
    }

    companion object{
        /**
         * Last-resort default, only used when the WebView version can't be read. Extensions
         * shouldn't set a user agent at all: every request already gets the app's one.
         */
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36"

        /** Default user agent, following the installed WebView's Chrome version. */
        val defaultUserAgent: String
            get() = WebViewUtil.defaultUserAgent
    }
}
