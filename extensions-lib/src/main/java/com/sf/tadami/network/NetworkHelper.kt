package com.sf.tadami.network

import android.content.Context
import okhttp3.OkHttpClient

/**
 * The app's HTTP clients, which an extension gets from Injekt: `Injekt.get<NetworkHelper>()`, or
 * [com.sf.tadami.source.online.AnimeHttpSource.network] in a source. Every request they make gets
 * the user agent of the app's settings and shares the cookies of its WebView.
 *
 * @since extensions-lib 1.5
 */
class NetworkHelper(private val context : Context) {

    /**
     * The client to use: with a cache, and solving the Cloudflare challenges in a WebView.
     *
     * @since extensions-lib 1.5
     */
    val client: OkHttpClient = throw Exception("Stub!")

    /**
     * [client] without the Cloudflare challenge solving, which fails on a challenge.
     *
     * @since extensions-lib 1.5
     */
    val nonCloudflareClient: OkHttpClient = throw Exception("Stub!")

    /**
     * The same as [client], which solves the Cloudflare challenges.
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("The regular client handles Cloudflare: use client", ReplaceWith("client"))
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
         *
         * @since extensions-lib 1.5
         */
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36"
    }
}
