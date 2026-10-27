package com.sf.tadami.network.interceptors

import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException

/**
 * Cloudflare marks challenge responses with `cf-mitigated: challenge`, body-less ones (HEAD)
 * included. Older setups are recognised from the challenge page itself; block pages (geo block,
 * banned IP) are left alone since a WebView can't do anything about them.
 */
public fun isCloudflareChallenge(response: Response): Boolean {
    if (response.header("cf-mitigated").equals("challenge", ignoreCase = true)) return true
    if (response.code !in ERROR_CODES) return false
    if (response.header("Server")?.startsWith("cloudflare", ignoreCase = true) != true) return false

    val body = try {
        response.peekBody(MAX_PEEK_BYTES).string()
    } catch (e: IOException) {
        return false
    }
    if ("window._cf_chl_opt" in body) return true
    val document = Jsoup.parse(body, response.request.url.toString())
    return document.getElementById("challenge-error-title") != null ||
        document.getElementById("challenge-error-text") != null
}

private val ERROR_CODES = listOf(403, 503)
private const val MAX_PEEK_BYTES = 256L * 1024
