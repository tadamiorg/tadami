package com.sf.tadami.data.download.stream

import androidx.annotation.StringRes
import com.sf.tadami.R
import com.sf.tadami.network.HttpException
import com.sf.tadami.source.model.StreamSource
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * The stream can't give a valid video: the next one is tried. [failure] is what the user is told,
 * in their language; [message] gives the details, in English, for the logs.
 */
class StreamFailedException(
    val failure: StreamFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Why a stream failed, as the user is told. */
enum class StreamFailure(@StringRes val text: Int) {
    PLAYLIST(R.string.download_stream_error_playlist),
    LIVE(R.string.download_stream_error_live),
    DRM(R.string.download_stream_error_drm),
    UNSUPPORTED(R.string.download_stream_error_unsupported),
    NOT_VIDEO(R.string.download_stream_error_not_video),
    DECRYPTION(R.string.download_stream_error_decryption),
    INCOMPLETE(R.string.download_stream_error_incomplete),
    TOO_SMALL(R.string.download_stream_error_too_small),
    TOO_SHORT(R.string.download_stream_error_too_short),
}

/** No stream is worth trying: no storage, no space left, no stream at all... */
open class DownloadFatalException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Writing to the storage folder failed, most likely full or gone. */
class StorageException(cause: Throwable) : DownloadFatalException(cause.message ?: cause.toString(), cause)

/** The device went offline: the download waits for the network, then resumes on the same stream. */
class NetworkLostException(cause: Throwable) : IOException(cause.message, cause)

/** What a download does after an error. */
enum class FailureAction {
    /** The stream is bad, the next one is tried */
    NEXT_STREAM,

    /** Only the network is missing: resume the same stream once it is back */
    WAIT_FOR_NETWORK,

    /** No other stream would do better */
    STOP,
}

object StreamFallback {

    /** Identifies a stream across fetches: its URL may be signed again each time. */
    fun key(stream: StreamSource): String = "${stream.server}|${stream.quality}|${stream.fullName}"

    /**
     * The streams in the order to try them: the one a download stopped on first, then the
     * extension's order (its preferences), the streams that already failed last.
     */
    fun attemptOrder(
        streams: List<StreamSource>,
        resumedKey: String?,
        failedKeys: Collection<String>,
    ): List<StreamSource> {
        val resumed = streams.firstOrNull { key(it) == resumedKey && resumedKey !in failedKeys }
        val others = streams.filter { it !== resumed }
        val (failed, untried) = others.partition { key(it) in failedKeys }
        return listOfNotNull(resumed) + untried + failed
    }

    fun classify(error: Throwable, online: Boolean): FailureAction {
        return when (error) {
            is CancellationException, is DownloadFatalException -> FailureAction.STOP
            is NetworkLostException -> FailureAction.WAIT_FOR_NETWORK
            is StreamFailedException, is HttpException -> FailureAction.NEXT_STREAM
            is IOException -> if (online) FailureAction.NEXT_STREAM else FailureAction.WAIT_FOR_NETWORK
            // Parsing errors, invalid URLs...
            else -> FailureAction.NEXT_STREAM
        }
    }

    /** Whether one request is worth sending again: network errors, timeouts, rate limits, server errors. */
    fun isRetryable(error: Throwable): Boolean {
        return when (error) {
            is HttpException -> error.code == 408 || error.code == 429 || error.code >= 500
            is NetworkLostException -> false
            is IOException -> true
            else -> false
        }
    }
}
