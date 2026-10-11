package com.sf.tadami.network

import android.util.Log
import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Shares cookies between OkHttp and every WebView through [CookieManager], so a Cloudflare
 * clearance obtained in a WebView is sent by OkHttp and vice versa.
 */
class AndroidCookieJar : CookieJar {

    /** Null when the device has no usable WebView; cookies then only live in memory. */
    private val manager: CookieManager? = try {
        CookieManager.getInstance()
    } catch (e: Throwable) {
        // MissingWebViewPackageException, WebView disabled or being updated
        Log.e("AndroidCookieJar", "WebView cookies unavailable", e)
        null
    }

    private val memoryCookies = ConcurrentHashMap<String, Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val manager = manager ?: return cookies.forEach { memoryCookies[it.key()] = it }
        val urlString = url.toString()

        cookies.forEach { manager.setCookie(urlString, it.toString()) }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return get(url)
    }

    fun get(url: HttpUrl): List<Cookie> {
        val manager = manager ?: return memoryCookies.values.filter {
            it.matches(url) && it.expiresAt > System.currentTimeMillis()
        }
        val cookies = manager.getCookie(url.toString())

        return if (cookies != null && cookies.isNotEmpty()) {
            cookies.split(";").mapNotNull { Cookie.parse(url, it.trim()) }
        } else {
            emptyList()
        }
    }

    /**
     * Expires the cookies named [cookieNames] (all of them when null) that [url] receives,
     * whether they were set for the host only or for one of its parent domains.
     *
     * @return the number of distinct cookie names removed
     */
    fun remove(url: HttpUrl, cookieNames: List<String>? = null, maxAge: Int = -1): Int {
        val names = get(url)
            .map { it.name }
            .distinct()
            .filter { cookieNames == null || it in cookieNames }
        if (names.isEmpty()) return 0

        val manager = manager ?: run {
            memoryCookies.values.removeAll { it.name in names && it.matches(url) }
            return names.size
        }

        val urlString = url.toString()
        val domains = cookieDomains(url)
        names.forEach { name ->
            // A deletion only applies to the cookie with the same name, domain and path
            manager.setCookie(urlString, "$name=; Max-Age=$maxAge")
            manager.setCookie(urlString, "$name=; Max-Age=$maxAge; Path=/")
            domains.forEach { domain ->
                manager.setCookie(urlString, "$name=; Max-Age=$maxAge; Domain=$domain; Path=/")
            }
        }
        manager.flush()
        return names.size
    }

    fun removeAll() {
        memoryCookies.clear()
        manager?.removeAllCookies {}
    }

    /** Persists cookies now instead of waiting for the WebView's periodic commit. */
    fun flush() {
        manager?.flush()
    }

    /** Whether a Cloudflare clearance is stored for [url] (e.g. one just obtained in the WebView). */
    fun hasCloudflareClearance(url: String): Boolean {
        val httpUrl = url.toHttpUrlOrNull() ?: return false
        return get(httpUrl).any { it.name == "cf_clearance" }
    }

    private fun Cookie.key(): String = "$domain|$path|$name"

    /** The host and each parent domain down to the registrable one, e.g. a.b.example.org, b.example.org, example.org. */
    private fun cookieDomains(url: HttpUrl): List<String> {
        val host = url.host
        val topPrivateDomain = url.topPrivateDomain() ?: return emptyList()
        val domains = mutableListOf<String>()
        var domain = host
        while (true) {
            domains += domain
            if (domain == topPrivateDomain || !domain.contains('.')) break
            domain = domain.substringAfter('.')
        }
        return domains
    }
}
