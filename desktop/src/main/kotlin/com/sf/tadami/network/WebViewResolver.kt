package com.sf.tadami.network

import okhttp3.Headers
import java.io.IOException

/**
 * The extension API's headless WebView, which the desktop doesn't have yet: [load] always fails, as
 * when the Android WebView can't be created.
 *
 * @since extensions-lib 1.6
 */
class WebViewResolver internal constructor() {

    class Result(
        val url: String,
        val html: String?,
        val scriptResult: String?,
        val interceptedUrl: String?,
        val timedOut: Boolean,
    )

    @JvmOverloads
    @Throws(IOException::class)
    fun load(
        url: String,
        headers: Headers = Headers.headersOf(),
        script: String? = null,
        interceptRequest: ((String) -> Boolean)? = null,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    ): Result = throw IOException("No WebView on the desktop yet")

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 30L
    }
}
