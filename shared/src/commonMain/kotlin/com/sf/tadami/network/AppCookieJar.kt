package com.sf.tadami.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** The cookies the OkHttp clients and the platform's browser share. */
interface AppCookieJar : CookieJar {

    /** The cookies [url] receives. */
    fun get(url: HttpUrl): List<Cookie>

    /**
     * Expires the cookies named [cookieNames] (all of them when null) that [url] receives,
     * whether they were set for the host only or for one of its parent domains.
     *
     * @return the number of distinct cookie names removed
     */
    fun remove(url: HttpUrl, cookieNames: List<String>? = null, maxAge: Int = -1): Int

    fun removeAll()

    /** Persists the cookies now. */
    fun flush()

    /** Whether a Cloudflare clearance is stored for [url]. */
    fun hasCloudflareClearance(url: String): Boolean {
        val httpUrl = url.toHttpUrlOrNull() ?: return false
        return get(httpUrl).any { it.name == "cf_clearance" }
    }
}
