package com.sf.tadami.network

import com.sf.tadami.preferences.advanced.AdvancedPreferences
import okhttp3.OkHttpClient

/**
 * The app's HTTP clients, which an extension gets from Injekt: `Injekt.get<NetworkHelper>()`, or
 * AnimeHttpSource.network in a source. Every request they make gets the user agent of the app's
 * settings and shares the cookies of its browser.
 */
expect class NetworkHelper {

    val cookieManager: AppCookieJar

    /** The advanced settings, the user agent among them, kept up to date. */
    val advancedPreferences: AdvancedPreferences

    /** The user agent to send for a request asking for [requested]: the current one instead of a replaced one. */
    fun resolveUserAgent(requested: String?): String

    /** [client] without the Cloudflare challenge solving, which fails on a challenge. */
    val nonCloudflareClient: OkHttpClient

    /** The client to use: with a cache, and solving the Cloudflare challenges when the platform can. */
    val client: OkHttpClient

    @Deprecated("The regular client handles Cloudflare by default")
    val cloudflareClient: OkHttpClient

    /** A headless browser sharing this client's cookies and user agent. */
    val webViewResolver: WebViewResolver

    companion object {
        /** The default user agent of the platform. */
        val defaultUserAgent: String
    }
}
