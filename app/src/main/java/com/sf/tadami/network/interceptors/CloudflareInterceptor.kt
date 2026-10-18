package com.sf.tadami.network.interceptors

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.sf.tadami.R
import com.sf.tadami.network.AndroidCookieJar
import com.sf.tadami.network.CloudflareChallengeException
import com.sf.tadami.network.utils.CLOUDFLARE_CHALLENGE_PROBE
import com.sf.tadami.network.utils.WebViewUtil
import com.sf.tadami.ui.utils.UiToasts
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

class CloudflareInterceptor @JvmOverloads constructor(
    private val context: Context,
    private val cookieManager: AndroidCookieJar,
    userAgent: () -> String,
    private val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
) : WebViewInterceptor(context, userAgent) {

    @JvmOverloads
    constructor(
        context: Context,
        cookieManager: AndroidCookieJar,
        userAgent: String,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
    ) : this(context, cookieManager, { userAgent }, timeoutSeconds)

    private val mainHandler = Handler(Looper.getMainLooper())

    /** One challenge resolution at a time per site; requests queued behind it reuse its clearance. */
    private val siteLocks = ConcurrentHashMap<String, ReentrantLock>()

    /** Sites whose challenge just failed, so queued requests fail fast instead of each opening a WebView. */
    private val failedUntil = ConcurrentHashMap<String, Long>()

    override fun shouldIntercept(response: Response): Boolean {
        return isCloudflareChallenge(response)
    }

    override fun intercept(
        chain: Interceptor.Chain,
        request: Request,
        response: Response
    ): Response {
        // The Cookie header is added by the network layer, below this interceptor
        val sentClearance = response.networkResponse?.request?.header("Cookie")?.clearanceValue()
        // After redirects the challenge (and so the clearance) belongs to the final URL, e.g. a
        // mirror domain, not to the requested one
        val challenged = response.request
        response.close()

        val site = challenged.url.topPrivateDomain() ?: challenged.url.host
        val lock = siteLocks.getOrPut(site) { ReentrantLock() }
        try {
            lock.lockUnlessCanceled(chain)
        } catch (e: InterruptedException) {
            throw InterruptedIOException("Interrupted while waiting for the Cloudflare challenge")
        }
        try {
            val current = currentClearance(challenged.url)
            // Otherwise another request solved the challenge while this one was in flight
            if (current == null || current == sentClearance) {
                if ((failedUntil[site] ?: 0L) > SystemClock.elapsedRealtime()) {
                    throw challengeException(challenged)
                }

                // A clearance Cloudflare just rejected must not be mistaken for a fresh one
                if (current != null) {
                    cookieManager.remove(challenged.url, COOKIE_NAMES, 0)
                }

                if (resolveWithWebView(chain, challenged, current) == Outcome.FAILED) {
                    failedUntil[site] = SystemClock.elapsedRealtime() + FAILURE_BACKOFF_MILLIS
                    // Prompt user to update WebView if it seems too outdated
                    if (WebViewUtil.isOutdated()) {
                        UiToasts.showToast(R.string.webview_outdated, Toast.LENGTH_LONG)
                    }
                    throw challengeException(challenged)
                }
                failedUntil.remove(site)
            }
        }
        // Because OkHttp's enqueue only handles IOExceptions, wrap the exception so that
        // we don't crash the entire app
        catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException(e)
        } finally {
            lock.unlock()
        }

        return chain.proceed(request)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(
        chain: Interceptor.Chain,
        request: Request,
        staleClearance: String?,
    ): Outcome {
        // OkHttp interceptors are synchronous, so this thread waits for the WebView
        val latch = CountDownLatch(1)
        val state = SolveState()
        val url = request.url
        val headers = parseHeaders(request.headers)

        // Only touched on the main thread
        var webView: WebView? = null

        fun isSolved(): Boolean {
            val clearance = currentClearance(url)
            return clearance != null && clearance != staleClearance
        }

        fun finish(outcome: Outcome) {
            if (state.outcome == null) {
                state.outcome = outcome
                latch.countDown()
            }
        }

        // Cloudflare already writes cf_clearance while its challenge runs: the cookie only counts
        // once the challenge let the page through
        fun checkPage(view: WebView) {
            view.evaluateJavascript(CLOUDFLARE_CHALLENGE_PROBE) { isChallenge ->
                when {
                    state.outcome != null || state.abandoned -> Unit
                    isChallenge == "true" -> state.challengeSeen = true
                    isSolved() -> finish(Outcome.SOLVED)
                    // The page loaded without any challenge, a plain retry will do
                    !state.challengeSeen -> finish(Outcome.NO_CHALLENGE)
                }
            }
        }

        // In case the challenge lets the page through without a load the client hears about
        val pollClearance = object : Runnable {
            override fun run() {
                if (state.outcome != null || state.abandoned) return
                val view = webView
                if (view != null && isSolved()) checkPage(view)
                mainHandler.postDelayed(this, POLL_MILLIS)
            }
        }

        mainHandler.post {
            // The caller gave up (canceled or timed out) before the main thread got here
            if (state.abandoned) return@post
            try {
                val view = createWebView(request)
                webView = view
                view.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, finishedUrl: String?) {
                        checkPage(view)
                    }

                    override fun onReceivedHttpError(
                        view: WebView,
                        request: WebResourceRequest,
                        errorResponse: WebResourceResponse,
                    ) {
                        if (!request.isForMainFrame) return
                        if (errorResponse.statusCode in ERROR_CODES) {
                            // Found the Cloudflare challenge page.
                            state.challengeSeen = true
                        } else {
                            // A regular HTTP error, the challenge isn't there anymore
                            finish(Outcome.NO_CHALLENGE)
                        }
                    }

                    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                        // Not handling it kills the app. The WebView can't be used anymore
                        Log.e(TAG, "WebView renderer gone while solving $url: crashed=${detail.didCrash()}")
                        if (webView === view) {
                            webView = null
                            view.destroy()
                        }
                        state.rendererGone = true
                        latch.countDown()
                        return true
                    }
                }
                view.loadUrl(url.toString(), headers)
                mainHandler.postDelayed(pollClearance, POLL_MILLIS)
            } catch (e: Throwable) {
                // WebView missing, disabled or being updated
                state.error = e
                latch.countDown()
            }
        }

        try {
            latch.awaitUnlessCanceled(chain, timeoutSeconds)
        } finally {
            state.abandoned = true
            mainHandler.removeCallbacks(pollClearance)
            mainHandler.post {
                // Also drops a poll scheduled by a creation that ran after the removal above
                mainHandler.removeCallbacks(pollClearance)
                webView?.run {
                    stopLoading()
                    destroy()
                }
                webView = null
            }
        }

        state.error?.let { throw IOException("Could not start the WebView", it) }
        // Not the challenge's fault: no backoff, the next request tries again
        if (state.outcome == null && state.rendererGone) throw IOException("The WebView renderer process is gone")
        val outcome = state.outcome ?: Outcome.FAILED
        if (outcome == Outcome.SOLVED) {
            // Keep the clearance if the app is killed before the WebView's periodic commit
            cookieManager.flush()
        } else if (outcome == Outcome.FAILED && currentClearance(url) != null) {
            // Written by a challenge that never let the page through: it would be rejected, and
            // would look like one the user obtained in the WebView
            cookieManager.remove(url, COOKIE_NAMES, 0)
        }
        return outcome
    }

    private fun currentClearance(url: HttpUrl): String? {
        return cookieManager.get(url).firstOrNull { it.name == CF_CLEARANCE }?.value
    }

    private fun challengeException(request: Request): CloudflareChallengeException {
        return CloudflareChallengeException(
            request.url.toString(),
            context.getString(R.string.request_bypass_cloudflare_failure),
        )
    }

    private fun ReentrantLock.lockUnlessCanceled(chain: Interceptor.Chain) {
        while (!tryLock(LOCK_SLICE_MILLIS, TimeUnit.MILLISECONDS)) {
            if (chain.call().isCanceled()) throw IOException("Canceled")
        }
    }

    private fun String.clearanceValue(): String? {
        return split(';')
            .map { it.trim() }
            .firstOrNull { it.startsWith("$CF_CLEARANCE=") }
            ?.substringAfter('=')
    }

    private enum class Outcome { SOLVED, NO_CHALLENGE, FAILED }

    private class SolveState {
        @Volatile var challengeSeen = false
        @Volatile var outcome: Outcome? = null
        @Volatile var error: Throwable? = null
        @Volatile var abandoned = false
        @Volatile var rendererGone = false
    }

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 30L
    }
}

/**
 * Cloudflare marks challenge responses with `cf-mitigated: challenge`, body-less ones (HEAD)
 * included. Older setups are recognised from the challenge page itself; block pages (geo block,
 * banned IP) are left alone since a WebView can't do anything about them.
 */
internal fun isCloudflareChallenge(response: Response): Boolean {
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

private const val TAG = "CloudflareInterceptor"
private val ERROR_CODES = listOf(403, 503)
private const val CF_CLEARANCE = "cf_clearance"
private val COOKIE_NAMES = listOf(CF_CLEARANCE)
private const val MAX_PEEK_BYTES = 256L * 1024
private const val POLL_MILLIS = 500L
private const val LOCK_SLICE_MILLIS = 250L
private const val FAILURE_BACKOFF_MILLIS = 60_000L
