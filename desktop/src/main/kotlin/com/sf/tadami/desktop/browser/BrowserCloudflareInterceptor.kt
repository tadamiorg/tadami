package com.sf.tadami.desktop.browser

import com.sf.tadami.network.AppCookieJar
import com.sf.tadami.network.CloudflareChallengeException
import com.sf.tadami.network.SolvingHosts
import com.sf.tadami.network.interceptors.isCloudflareChallenge
import com.sf.tadami.network.utils.CLOUDFLARE_CHALLENGE_PROBE
import com.sf.tadami.network.utils.CLOUDFLARE_INTERACTIVE_LISTENER
import com.sf.tadami.network.utils.CLOUDFLARE_INTERACTIVE_PROBE
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.request_bypass_cloudflare_failure
import com.sf.tadami.utils.Log
import com.sf.tadami.utils.stringRes
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/**
 * Solves the Cloudflare challenges in a page of the embedded browser, as the Android app's
 * CloudflareInterceptor does in a WebView: the page acts as the browser of the request's user agent, the
 * clearance it gets is copied to the OkHttp cookie jar and the request sent again.
 */
class BrowserCloudflareInterceptor(
    private val cookieJar: AppCookieJar,
    private val userAgent: () -> String,
    private val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
) : Interceptor {

    /** One challenge resolution at a time per site; requests queued behind it reuse its clearance. */
    private val siteLocks = ConcurrentHashMap<String, ReentrantLock>()

    /** Sites whose challenge just failed, so queued requests fail fast instead of each opening a page. */
    private val failedUntil = ConcurrentHashMap<String, Long>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!isCloudflareChallenge(response)) return response

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
                if ((failedUntil[site] ?: 0L) > System.currentTimeMillis()) {
                    throw challengeException(challenged)
                }
                // A clearance Cloudflare just rejected must not be mistaken for a fresh one
                if (current != null) cookieJar.remove(challenged.url, COOKIE_NAMES, 0)

                // The requested host too: the challenge may be on a mirror it redirected to
                val outcome = SolvingHosts.app.solving(listOf(request.url.host, challenged.url.host)) {
                    solve(chain, challenged, current)
                }
                if (outcome == Outcome.FAILED) {
                    failedUntil[site] = System.currentTimeMillis() + FAILURE_BACKOFF_MILLIS
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

    private fun solve(chain: Interceptor.Chain, request: Request, staleClearance: String?): Outcome {
        val url = request.url
        // Read by the polls of this thread, written by the page's
        val loaded = AtomicBoolean(false)
        val challengeSeen = AtomicBoolean(false)
        val page = HeadlessPage(
            userAgent = request.header("User-Agent")?.takeIf { it.isNotBlank() } ?: userAgent(),
            headers = request.headers.toPageHeaders(),
            onLoaded = { _, status ->
                // Cloudflare answers its challenge page with one of these
                if (status in ERROR_CODES) challengeSeen.set(true)
                loaded.set(true)
            },
        )
        try {
            // Hears the widget from the start of each document
            page.browser.devTools("Page.enable")
            page.browser.devTools(
                "Page.addScriptToEvaluateOnNewDocument",
                buildJsonObject { put("source", CLOUDFLARE_INTERACTIVE_LISTENER) },
            )
            page.load(url, cookieJar)
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
            while (System.nanoTime() < deadline) {
                if (chain.call().isCanceled()) throw IOException("Canceled")
                if (page.rendererGone) throw IOException("The browser's renderer process is gone")
                Thread.sleep(POLL_MILLIS)
                if (!loaded.get()) continue
                // Cloudflare already writes cf_clearance while its challenge runs: the cookie only
                // counts once the challenge let the page through
                val isChallenge = page.browser.evaluate(CLOUDFLARE_CHALLENGE_PROBE) ?: continue
                if (isChallenge.isTrue()) {
                    challengeSeen.set(true)
                    // No headless solve passes it: the user can, in the WebView
                    if (page.browser.evaluate(CLOUDFLARE_INTERACTIVE_PROBE).isTrue()) {
                        Log.i(TAG, "The Cloudflare challenge of $url wants a click: not waiting for the timeout")
                        return Outcome.FAILED
                    }
                    continue
                }
                val clearance = page.browser.cookies(url).firstOrNull { it.name == CF_CLEARANCE }?.value
                when {
                    clearance != null && clearance != staleClearance -> {
                        page.browser.pullCookies(url, cookieJar)
                        return Outcome.SOLVED
                    }
                    // The page loaded without any challenge, a plain retry will do
                    !challengeSeen.get() -> {
                        page.browser.pullCookies(url, cookieJar)
                        return Outcome.NO_CHALLENGE
                    }
                }
            }
        } catch (e: InterruptedException) {
            throw InterruptedIOException("Interrupted while solving the Cloudflare challenge")
        } finally {
            page.close()
        }
        Log.w(TAG, "Cloudflare challenge not solved in ${timeoutSeconds}s: $url")
        return Outcome.FAILED
    }

    private fun currentClearance(url: HttpUrl): String? {
        return cookieJar.get(url).firstOrNull { it.name == CF_CLEARANCE }?.value
    }

    private fun challengeException(request: Request): CloudflareChallengeException {
        return CloudflareChallengeException(
            request.url.toString(),
            stringRes(Res.string.request_bypass_cloudflare_failure),
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

    companion object {
        const val DEFAULT_TIMEOUT_SECONDS = 30L
    }
}

/** The headers a page request can carry: the browser sets the user agent, the cookies and the transport ones. */
internal fun Headers.toPageHeaders(): Map<String, String> =
    names().filter { it.lowercase() !in UNSAFE_HEADERS }.associateWith { get(it)!! }

private val UNSAFE_HEADERS = setOf(
    "user-agent", "cookie", "host", "connection", "content-length", "accept-encoding",
    "sec-ch-ua", "sec-ch-ua-mobile", "sec-ch-ua-platform", "x-requested-with",
)

private const val TAG = "BrowserCloudflare"
private val ERROR_CODES = listOf(403, 503)
private const val CF_CLEARANCE = "cf_clearance"
private val COOKIE_NAMES = listOf(CF_CLEARANCE)
private const val POLL_MILLIS = 500L
private const val LOCK_SLICE_MILLIS = 250L
private const val FAILURE_BACKOFF_MILLIS = 60_000L
