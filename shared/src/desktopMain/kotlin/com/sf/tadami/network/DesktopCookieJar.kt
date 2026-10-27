package com.sf.tadami.network

import okhttp3.Cookie
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * The cookies of the OkHttp clients, also behind the android.webkit.CookieManager shim the
 * extensions call: a cookie one sets is sent by the other. In memory only for now.
 */
object DesktopCookieJar : AppCookieJar {

    private val cookies = ConcurrentHashMap<String, Cookie>()

    private fun Cookie.key() = "$domain|$path|$name"

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { this.cookies[it.key()] = it }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = get(url)

    override fun get(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        cookies.values.removeIf { it.expiresAt <= now }
        return cookies.values.filter { it.matches(url) }
    }

    override fun remove(url: HttpUrl, cookieNames: List<String>?, maxAge: Int): Int {
        val removed = get(url).filter { cookieNames == null || it.name in cookieNames }
        removed.forEach { cookies.remove(it.key()) }
        return removed.map { it.name }.distinct().size
    }

    override fun removeAll() {
        cookies.clear()
    }

    override fun flush() = Unit
}
