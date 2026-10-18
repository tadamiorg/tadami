package com.sf.tadami.network

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.sf.tadami.network.interceptors.toWebViewHeaders
import com.sf.tadami.network.utils.CLOUDFLARE_CHALLENGE_PROBE
import com.sf.tadami.network.utils.applyUserAgent
import com.sf.tadami.network.utils.setDefaultSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Headers
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Loads a page in a headless WebView that shares the app's cookies (Cloudflare clearance included)
 * and user agent, for sources whose content only exists once JavaScript has run: players injected
 * by scripts, links computed at runtime, requests fired by an embedded player...
 *
 * Cloudflare challenges met on the way are waited through like in a browser, and the clearance
 * they give is then also used by the OkHttp clients.
 *
 * Blocking: call it from a background thread, never from the main thread.
 *
 * @since extensions-lib 1.6
 */
class WebViewResolver internal constructor(
    private val context: Context,
    private val cookieJar: AndroidCookieJar,
    private val defaultUserAgent: () -> String,
) {

    private val mainHandler = Handler(Looper.getMainLooper())

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
     * @throws IOException if the WebView can't be created, its renderer process dies or the thread is
     * interrupted
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun load(
        url: String,
        headers: Headers = Headers.headersOf(),
        script: String? = null,
        interceptRequest: ((String) -> Boolean)? = null,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    ): Result {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "WebViewResolver.load blocks, call it from a background thread"
        }

        val latch = CountDownLatch(1)
        val state = State(url)
        val bridge = ScriptBridge(state)
        val userAgent = headers["User-Agent"]?.takeIf { it.isNotBlank() } ?: defaultUserAgent()
        val webViewHeaders = headers.newBuilder().removeAll("User-Agent").build().toWebViewHeaders()

        // Only touched on the main thread
        var webView: WebView? = null

        fun finish() {
            if (!state.done) {
                state.done = true
                latch.countDown()
            }
        }

        fun captureHtml(view: WebView, finishAfter: Boolean) {
            view.evaluateJavascript("document.documentElement.outerHTML") { raw ->
                state.html = decodeJsString(raw)
                if (finishAfter) finish()
            }
        }

        bridge.listener = { generation, result ->
            mainHandler.post {
                val view = webView
                if (view != null && !state.done && generation == state.generation) {
                    state.scriptResult = result
                    captureHtml(view, finishAfter = interceptRequest == null)
                }
            }
        }

        mainHandler.post {
            try {
                val view = WebView(context)
                webView = view
                view.setDefaultSettings()
                view.applyUserAgent(userAgent)
                if (script != null) view.addJavascriptInterface(bridge, BRIDGE_NAME)
                view.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, pageUrl: String?, favicon: Bitmap?) {
                        // A new document: results from the previous one are stale
                        state.generation++
                    }

                    override fun onPageFinished(view: WebView, pageUrl: String?) {
                        if (state.done) return
                        val generation = state.generation
                        // Look at the document itself: the HTTP error and page start callbacks
                        // don't arrive in a reliable order across WebView versions
                        view.evaluateJavascript(CLOUDFLARE_CHALLENGE_PROBE) { isChallenge ->
                            // Cloudflare reloads the page once the challenge is solved
                            if (state.done || generation != state.generation || isChallenge == "true") {
                                return@evaluateJavascript
                            }
                            pageUrl?.let { state.url = it }
                            when {
                                script != null -> view.evaluateJavascript(wrapScript(script, generation), null)
                                else -> captureHtml(view, finishAfter = interceptRequest == null)
                            }
                        }
                    }

                    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                        // Not handling it kills the app. The WebView can't be used anymore
                        Log.e(TAG, "WebView renderer gone while loading $url: crashed=${detail.didCrash()}")
                        if (webView === view) {
                            webView = null
                            view.destroy()
                        }
                        if (!state.done) {
                            state.rendererGone = true
                            finish()
                        }
                        return true
                    }

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        if (interceptRequest != null && !state.done) {
                            val requestUrl = request.url.toString()
                            if (runCatching { interceptRequest(requestUrl) }.getOrDefault(false)) {
                                state.interceptedUrl = requestUrl
                                finish()
                            }
                        }
                        return null
                    }
                }
                view.loadUrl(url, webViewHeaders)
            } catch (e: Throwable) {
                state.error = e
                latch.countDown()
            }
        }

        val completed = try {
            latch.await(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            throw InterruptedIOException("Interrupted while loading $url")
        } finally {
            mainHandler.post {
                webView?.run {
                    stopLoading()
                    if (script != null) removeJavascriptInterface(BRIDGE_NAME)
                    destroy()
                }
                webView = null
            }
        }

        state.error?.let { throw IOException("Could not start the WebView", it) }
        if (state.rendererGone) throw IOException("The WebView renderer process is gone while loading $url")
        // A challenge solved on the way gave a clearance worth keeping
        cookieJar.flush()

        return Result(
            url = state.url,
            html = state.html,
            scriptResult = state.scriptResult,
            interceptedUrl = state.interceptedUrl,
            timedOut = !completed || state.error != null,
        )
    }

    private class State(initialUrl: String) {
        @Volatile var url: String = initialUrl
        @Volatile var html: String? = null
        @Volatile var scriptResult: String? = null
        @Volatile var interceptedUrl: String? = null
        @Volatile var generation = 0
        @Volatile var done = false
        @Volatile var error: Throwable? = null
        @Volatile var rendererGone = false
    }

    /** Receives script results on the JavaBridge thread. */
    private class ScriptBridge(private val state: State) {
        var listener: (generation: Int, result: String?) -> Unit = { _, _ -> }

        @JavascriptInterface
        fun onResult(generation: Int, result: String?) {
            if (!state.done) listener(generation, result)
        }
    }

    private fun wrapScript(script: String, generation: Int): String {
        return """
            (async () => {
              let result = null;
              try {
                const value = await ($script);
                if (value !== undefined && value !== null) {
                  result = typeof value === 'string' ? value : JSON.stringify(value);
                }
              } catch (e) {}
              $BRIDGE_NAME.onResult($generation, result);
            })();
        """.trimIndent()
    }

    private fun decodeJsString(raw: String?): String? {
        if (raw == null) return null
        return try {
            when (val element = Json.parseToJsonElement(raw)) {
                is JsonNull -> null
                is JsonPrimitive -> element.content
                else -> element.toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 30L
        private const val BRIDGE_NAME = "__tadamiResolver"
        private const val TAG = "WebViewResolver"
    }
}
