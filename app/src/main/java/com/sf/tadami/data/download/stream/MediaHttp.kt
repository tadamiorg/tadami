package com.sf.tadami.data.download.stream

import com.sf.tadami.network.HttpException
import com.sf.tadami.network.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * The requests of a download: the stream's headers on the app client, no compression (Range
 * offsets are about the raw bytes), retries for what may pass on a second try.
 */
class MediaHttp(
    private val client: OkHttpClient,
    private val firstRetryDelayMs: Long = 1000L,
) {

    /** A successful response, after retries; the caller closes it. */
    suspend fun open(url: String, headers: Headers?, range: String? = null): Response {
        return retrying { openOnce(url, headers, range) }
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
        return retrying {
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
                // A blocked socket read only ends this way
                response.close()
            }
        }
        try {
            withContext(Dispatchers.IO) { block(response) }
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
            response.close()
            throw HttpException(response.code)
        }
        return response
    }

    private suspend fun <T> retrying(block: suspend () -> T): T {
        var delayMs = firstRetryDelayMs
        repeat(ATTEMPTS - 1) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!StreamFallback.isRetryable(e)) throw e
            }
            delay(delayMs)
            delayMs *= 2
        }
        return block()
    }

    private companion object {
        const val ATTEMPTS = 3
    }
}
