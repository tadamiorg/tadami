package com.sf.tadami.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

/**
 * The cookies of the OkHttp clients, also behind the android.webkit.CookieManager shim the
 * extensions call: a cookie one sets is sent by the other. In memory only for now.
 */
object DesktopCookieJar : CookieJar {

    private val cookies = ConcurrentHashMap<String, Cookie>()

    private fun Cookie.key() = "$domain|$path|$name"

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { this.cookies[it.key()] = it }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = get(url)

    fun get(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        cookies.values.removeIf { it.expiresAt <= now }
        return cookies.values.filter { it.matches(url) }
    }
}
