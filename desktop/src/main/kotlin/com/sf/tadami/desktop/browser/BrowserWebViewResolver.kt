package com.sf.tadami.desktop.browser

import com.sf.tadami.network.AppCookieJar
import com.sf.tadami.network.WebViewResolver
import com.sf.tadami.network.utils.CLOUDFLARE_CHALLENGE_PROBE
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** The pages of [WebViewResolver] in the embedded browser, as the Android app's AndroidWebViewResolver runs them. */
class BrowserWebViewResolver(
    private val cookieJar: AppCookieJar,
    private val defaultUserAgent: () -> String,
) : WebViewResolver() {

    @Throws(IOException::class)
    override fun load(
        url: String,
        headers: Headers,
        script: String?,
        interceptRequest: ((String) -> Boolean)?,
        timeoutSeconds: Long,
    ): Result {
        val pageUrl = url.toHttpUrl()
        val latch = CountDownLatch(1)
        val state = State(url)
        // A new document: results from the previous one are stale
        val generation = AtomicInteger()

        fun finish() {
            if (!state.done) {
                state.done = true
                latch.countDown()
            }
        }

        var page: HeadlessPage? = null
        val created = HeadlessPage(
            userAgent = headers["User-Agent"]?.takeIf { it.isNotBlank() } ?: defaultUserAgent(),
            headers = headers.toPageHeaders(),
            onLoaded = onLoaded@{ loadedUrl, _ ->
                val browser = page?.browser ?: return@onLoaded
                val current = generation.incrementAndGet()
                if (state.done) return@onLoaded
                // Cloudflare reloads the page once the challenge is solved
                if (browser.evaluate(CLOUDFLARE_CHALLENGE_PROBE).isTrue()) return@onLoaded
                state.url = loadedUrl
                if (script != null) {
                    val result = browser.evaluate(wrapScript(script), awaitPromise = true)
                    if (state.done || current != generation.get()) return@onLoaded
                    state.scriptResult = result?.asScriptResult()
                }
                state.html = browser.evaluate("document.documentElement.outerHTML")?.asScriptResult()
                if (interceptRequest == null) finish()
            },
            onRequest = { requestUrl ->
                if (interceptRequest != null && !state.done &&
                    runCatching { interceptRequest(requestUrl) }.getOrDefault(false)
                ) {
                    state.interceptedUrl = requestUrl
                    finish()
                }
            },
        )
        page = created

        val completed = try {
            created.load(pageUrl, cookieJar)
            awaitUnlessGone(latch, created, timeoutSeconds)
        } catch (e: InterruptedException) {
            throw InterruptedIOException("Interrupted while loading $url")
        } finally {
            // A challenge solved on the way gave a clearance worth keeping
            runCatching { created.browser.pullCookies(pageUrl, cookieJar) }
            created.close()
        }
        if (created.rendererGone) throw IOException("The browser's renderer process is gone while loading $url")

        return Result(
            url = state.url,
            html = state.html,
            scriptResult = state.scriptResult,
            interceptedUrl = state.interceptedUrl,
            timedOut = !completed,
        )
    }

    private fun awaitUnlessGone(latch: CountDownLatch, page: HeadlessPage, timeoutSeconds: Long): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
        while (System.nanoTime() < deadline) {
            if (latch.await(SLICE_MILLIS, TimeUnit.MILLISECONDS)) return true
            if (page.rendererGone) return false
        }
        return false
    }

    private class State(initialUrl: String) {
        @Volatile var url: String = initialUrl
        @Volatile var html: String? = null
        @Volatile var scriptResult: String? = null
        @Volatile var interceptedUrl: String? = null
        @Volatile var done = false
    }

    /** Its value, null when it throws: as the Android resolver, which runs it through a bridge. */
    private fun wrapScript(script: String): String {
        return """
            (async () => {
              try {
                const value = await ($script);
                return value === undefined ? null : value;
              } catch (e) {
                return null;
              }
            })()
        """.trimIndent()
    }

    private companion object {
        const val SLICE_MILLIS = 250L
    }
}
