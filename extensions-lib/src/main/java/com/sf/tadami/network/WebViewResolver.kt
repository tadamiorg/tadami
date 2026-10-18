package com.sf.tadami.network

import okhttp3.Headers

/**
 * Loads a page in a headless WebView that shares the app's cookies (Cloudflare clearance included)
 * and user agent, for pages whose content only exists once JavaScript has run. Cloudflare
 * challenges met on the way are waited through, and their clearance is reused by the OkHttp
 * clients.
 *
 * Blocking: call it from a background thread, never from the main thread.
 *
 * @since extensions-lib 1.6: an extension using it must set apiVersion to 1.6 or above
 */
class WebViewResolver private constructor() {

    /**
     * @property url the last page loaded in the main frame
     * @property html the final DOM of that page, or null if it never finished loading
     * @property scriptResult what the script evaluated to (JSON for non-string values), or null
     * @property interceptedUrl the first request accepted by `interceptRequest`, or null
     * @property timedOut whether [load] gave up before getting what it waited for
     */
    class Result private constructor() {
        val url: String = throw Exception("Stub!")
        val html: String? = throw Exception("Stub!")
        val scriptResult: String? = throw Exception("Stub!")
        val interceptedUrl: String? = throw Exception("Stub!")
        val timedOut: Boolean = throw Exception("Stub!")
    }

    /**
     * Loads [url] and waits until [interceptRequest] accepts a request made by the page, when
     * given, or else until the page finished loading and [script], when given, produced a value.
     *
     * @param headers sent with the page request; its User-Agent, if any, replaces the app's one
     * @param script a JavaScript expression evaluated once the page has loaded (again after each
     * navigation). It may evaluate to a Promise, e.g. one polling for an iframe added later.
     * @param interceptRequest called for every request the page makes (from a background thread)
     * @throws java.io.IOException if the WebView can't be created or the thread is interrupted
     */
    fun load(
        url: String,
        headers: Headers = Headers.headersOf(),
        script: String? = null,
        interceptRequest: ((String) -> Boolean)? = null,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    ): Result = throw Exception("Stub!")

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 30L
    }
}
