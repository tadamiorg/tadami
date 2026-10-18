package com.sf.tadami.network

import android.content.Context
import okhttp3.OkHttpClient

class NetworkHelper(private val context : Context) {

    val client: OkHttpClient = throw Exception("Stub!")

    val nonCloudflareClient: OkHttpClient = throw Exception("Stub!")

    val cloudflareClient: OkHttpClient = throw Exception("Stub!")

    /**
     * Headless WebView sharing the app's cookies and user agent.
     *
     * @since extensions-lib 1.6: an extension using it must set apiVersion to 1.6 or above
     */
    val webViewResolver: WebViewResolver = throw Exception("Stub!")

    companion object{
        /**
         * Compiled into the extension: it never follows the app's user agent. Prefer not setting a
         * User-Agent at all, every request already gets the app's one.
         */
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36"
    }
}
