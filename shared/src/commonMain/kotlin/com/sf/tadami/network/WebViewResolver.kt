package com.sf.tadami.network

import okhttp3.Headers
import java.io.IOException

/**
 * Loads a page in a headless browser that shares the app's cookies (Cloudflare clearance included)
 * and user agent, for sources whose content only exists once JavaScript has run: players injected
 * by scripts, links computed at runtime, requests fired by an embedded player...
 *
 * Cloudflare challenges met on the way are waited through like in a browser, and the clearance
 * they give is then also used by the OkHttp clients.
 *
 * Blocking: call it from a background thread, never from the main thread. Each platform provides
 * its browser by overriding [load]; one without any fails every load.
 *
 * @since extensions-lib 1.6
 */
open class WebViewResolver protected constructor() {

    /**
     * @property url the last page loaded in the main frame
     * @property html the final DOM of that page, or null if it never finished loading
     * @property scriptResult what the script evaluated to (JSON for non-string values), or null
     * @property interceptedUrl the first request accepted by `interceptRequest`, or null
     * @property timedOut whether [load] gave up before getting what it waited for
     */
    class Result(
        val url: String,
        val html: String?,
        val scriptResult: String?,
        val interceptedUrl: String?,
        val timedOut: Boolean,
    )

    /**
     * Loads [url] and waits until:
     * - [interceptRequest] accepts a request made by the page, when given, or
     * - the page finished loading and [script], when given, produced a value.
     *
     * @param headers sent with the page request; its User-Agent, if any, replaces the app's one
     * @param script a JavaScript expression evaluated once the page has loaded (again after each
     * navigation). It may evaluate to a Promise, e.g. one polling for an iframe added later.
     * @param interceptRequest called for every request the page makes (from a background thread)
     * @throws IOException if the browser can't be created, its renderer process dies or the thread is
     * interrupted
     */
    @JvmOverloads
    @Throws(IOException::class)
    open fun load(
        url: String,
        headers: Headers = Headers.headersOf(),
        script: String? = null,
        interceptRequest: ((String) -> Boolean)? = null,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    ): Result = throw IOException("No browser to load $url")

    /** The resolver of a platform without a browser: every load fails. */
    class Unavailable : WebViewResolver()

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 30L
    }
}
