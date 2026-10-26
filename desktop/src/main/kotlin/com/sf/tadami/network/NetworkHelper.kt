package com.sf.tadami.network

import com.sf.tadami.desktop.AppDirs
import com.sf.tadami.network.interceptors.IgnoreGzipInterceptor
import com.sf.tadami.network.interceptors.UncaughtExceptionInterceptor
import com.sf.tadami.network.interceptors.UserAgentInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The desktop's HTTP clients, at the class name the extensions get from Injekt. They send the same
 * mobile user agent as the Android app: the sites serve the pages the extensions were written for.
 * No Cloudflare solving yet: [client] fails on a challenge like [nonCloudflareClient].
 */
class NetworkHelper {

    val cookieManager = DesktopCookieJar

    val userAgent: String = DEFAULT_USER_AGENT

    private val clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
        .cookieJar(cookieManager)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES)
        .cache(Cache(directory = File(AppDirs.cache, "network_cache"), maxSize = 5L * 1024 * 1024))
        .addInterceptor(UncaughtExceptionInterceptor())
        .addInterceptor(UserAgentInterceptor(userAgent = { userAgent }, replaces = { false }))
        .addNetworkInterceptor(IgnoreGzipInterceptor())
        .addNetworkInterceptor(BrotliInterceptor)

    val nonCloudflareClient = clientBuilder.build()

    val client = nonCloudflareClient

    @Deprecated("The regular client handles Cloudflare by default")
    @Suppress("UNUSED")
    val cloudflareClient: OkHttpClient = client

    /** No headless browser on the desktop yet: every load fails. */
    val webViewResolver: WebViewResolver by lazy { WebViewResolver() }

    companion object {
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36"
    }
}
