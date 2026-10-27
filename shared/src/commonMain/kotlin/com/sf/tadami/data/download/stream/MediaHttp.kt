package com.sf.tadami.data.download.stream

import com.sf.tadami.network.HttpException
import com.sf.tadami.network.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap

/**
 * The requests of a download: the stream's headers on the app client, no compression (Range
 * offsets are about the raw bytes), retries for what may pass on a second try.
 *
 * A rate limited host (429) is waited for by all the requests to it, at least as long as it asks
 * with Retry-After and longer at each new 429: the video hosts block an IP for 10 s to a few
 * minutes, the other qualities of the stream on the same host too.
 */
class MediaHttp(
    private val client: OkHttpClient,
    private val firstRetryDelayMs: Long = 1000L,
    private val firstRateLimitDelayMs: Long = 5000L,
) {

    /** Until when each host asked to wait, on the [now] clock */
    private val hostWaits = ConcurrentHashMap<String, Long>()

    /** A successful response, after retries; the caller closes it. */
    suspend fun open(url: String, headers: Headers?, range: String? = null): Response {
        return retrying(url) { openOnce(url, headers, range) }
    }

    /**
     * Runs [block] on the response, retrying the whole when it fails with a retryable error.
     * Cancelling the coroutine cancels the call, even while the body is read.
     */
    suspend fun <T> read(
        url: String,
        headers: Headers?,
        range: String? = null,
        block: (Response) -> T,
    ): T {
        return retrying(url) {
            openOnce(url, headers, range).use { response -> readCancellable(response, block) }
        }
    }

    suspend fun bytes(url: String, headers: Headers?, range: String? = null): ByteArray {
        return read(url, headers, range) { it.body.bytes() }
    }

    /** Reads [response] with [block] on the IO dispatcher, cancelling its call when the coroutine is. */
    suspend fun <T> readCancellable(response: Response, block: (Response) -> T): T = coroutineScope {
        val canceller = launch {
            try {
                awaitCancellation()
            } finally {
                // A blocked socket read only ends this way. Closing it during the read may throw:
                // that is not the read's failure
                runCatching { response.close() }
            }
        }
        try {
            withContext(Dispatchers.IO) { block(response) }
        } catch (e: Exception) {
            // Closed by the canceller: the read fails with "closed", but it is a cancellation
            ensureActive()
            throw e
        } finally {
            canceller.cancel()
        }
    }

    private suspend fun openOnce(url: String, headers: Headers?, range: String?): Response {
        val request = Request.Builder()
            .url(url)
            .apply {
                headers?.let { headers(it) }
                header("Accept-Encoding", "identity")
                range?.let { header("Range", it) }
            }
            .build()
        val response = client.newCall(request).await()
        if (!response.isSuccessful) {
            if (response.code == 429) {
                retryAfterMs(response.headers, System.currentTimeMillis())?.let { hold(request.url.host, it) }
            }
            response.close()
            throw HttpException(response.code)
        }
        return response
    }

    private suspend fun <T> retrying(url: String, block: suspend () -> T): T {
        val host = url.toHttpUrl().host
        var delayMs = firstRetryDelayMs
        var failures = 0
        var rateLimits = 0
        while (true) {
            awaitHost(host)
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!StreamFallback.isRetryable(e)) throw e
                if (e is HttpException && e.code == 429) {
                    if (++rateLimits > RATE_LIMIT_RETRIES) throw e
                    // At least the Retry-After: the parallel requests to the host are at the same count
                    hold(host, firstRateLimitDelayMs shl (rateLimits - 1))
                } else {
                    if (++failures >= ATTEMPTS) throw e
                    delay(delayMs)
                    delayMs *= 2
                }
            }
        }
    }

    private fun now() = System.nanoTime() / 1_000_000

    private fun hold(host: String, ms: Long) {
        hostWaits.merge(host, now() + ms) { current, asked -> maxOf(current, asked) }
    }

    private suspend fun awaitHost(host: String) {
        while (true) {
            val waitMs = (hostWaits[host] ?: return) - now()
            if (waitMs <= 0) return
            delay(waitMs)
        }
    }

    companion object {
        private const val ATTEMPTS = 3

        /** The 429s a request waits out before it fails */
        private const val RATE_LIMIT_RETRIES = 4

        /** Longer asked waits are cut to this: past it the download had better fail */
        const val MAX_RETRY_AFTER_MS = 5 * 60 * 1000L

        /** The wait a Retry-After header asks for, given in seconds or as a date; null without one. */
        fun retryAfterMs(headers: Headers, nowMs: Long): Long? {
            val value = headers["Retry-After"]?.trim() ?: return null
            val ms = value.toLongOrNull()?.times(1000)
                ?: headers.getDate("Retry-After")?.let { it.time - nowMs }
                ?: return null
            return ms.coerceIn(0, MAX_RETRY_AFTER_MS)
        }
    }
}
