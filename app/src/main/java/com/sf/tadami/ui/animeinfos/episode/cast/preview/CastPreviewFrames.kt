package com.sf.tadami.ui.animeinfos.episode.cast.preview

import com.sf.tadami.ui.animeinfos.episode.player.preview.PREVIEW_FRAME_CACHE_BYTES
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewFrameCache
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewFrameKey

/**
 * The phone-side frame cache of the cast session, and which request to decode next.
 *
 * Scoped to the ONE stream the TV plays (the requests' sourceId, which changes with the episode and
 * the source): a request about another stream drops every frame, and the session's end drops them
 * all ([clear]). It is never a media registry: it holds JPEG frames, keyed by preview step, nothing
 * about how to reach the stream (each request carries that).
 *
 * As in the phone player: frames are kept LRU within [PREVIEW_FRAME_CACHE_BYTES]; a step that gave
 * no frame isn't decoded again; one decode at a time, the latest request waiting meanwhile (an older
 * waiting one is dropped: the receiver asks one at a time and gives up on its own). Main thread only.
 */
class CastPreviewFrames(maxBytes: Long = PREVIEW_FRAME_CACHE_BYTES) {

    sealed interface Action {
        /** Answer [request] now with [status] (and [frame] when it's ok). */
        class Reply(val request: CastPreviewRequest, val status: String, val frame: PreviewJpeg? = null) : Action

        /** Decode the frame of [request]'s step. */
        class Fetch(val request: CastPreviewRequest) : Action
    }

    private val frames = PreviewFrameCache<PreviewJpeg>(maxBytes) { it.bytes.size.toLong() }

    // Steps that gave no frame in time: not decoded again for this stream
    private val unavailable = mutableSetOf<Int>()

    // The stream can't be decoded: every request about it is answered "off"
    private var failed = false

    /** The request being decoded. */
    var inFlight: CastPreviewRequest? = null
        private set

    private var waiting: CastPreviewRequest? = null

    /** The stream whose frames are kept, null before the first request or after [clear]. */
    val sourceId: String?
        get() = frames.sourceId

    val size: Int
        get() = frames.size

    val bytes: Long
        get() = frames.bytes

    /**
     * A request from the receiver: its reply when it's known (cached frame, frameless step, stream
     * off), the decode to start, or null when it waits for the decode in flight.
     */
    fun onRequest(request: CastPreviewRequest): Action? {
        if (request.sourceId != frames.sourceId) switchTo(request.sourceId)
        known(request)?.let { return it }
        if (inFlight != null) {
            waiting = request
            return null
        }
        inFlight = request
        return Action.Fetch(request)
    }

    /**
     * The decode of [request] ended with [frame], or none ([sourceFailed]: the stream can't be decoded
     * at all). Returns the reply, then what the waiting request needs; nothing when the stream changed
     * meanwhile (that decode is dropped).
     */
    fun onFetched(request: CastPreviewRequest, frame: PreviewJpeg?, sourceFailed: Boolean = false): List<Action> {
        if (inFlight !== request) return emptyList()
        inFlight = null
        val actions = mutableListOf<Action>()
        actions += when {
            sourceFailed -> {
                failed = true
                Action.Reply(request, CastPreviewProtocol.STATUS_OFF)
            }
            frame != null -> {
                frames.put(PreviewFrameKey(request.sourceId, request.step), frame)
                Action.Reply(request, CastPreviewProtocol.STATUS_OK, frame)
            }
            else -> {
                unavailable += request.step
                Action.Reply(request, CastPreviewProtocol.STATUS_NONE)
            }
        }
        waiting?.let { next ->
            waiting = null
            onRequest(next)?.let(actions::add)
        }
        return actions
    }

    /** The cast session ended: every frame dropped. */
    fun clear() {
        switchTo(null)
    }

    /** Memory pressure: the frames go, what is known about the stream stays. */
    fun dropFrames() {
        frames.clear()
    }

    private fun known(request: CastPreviewRequest): Action.Reply? {
        if (failed) return Action.Reply(request, CastPreviewProtocol.STATUS_OFF)
        frames[PreviewFrameKey(request.sourceId, request.step)]?.let {
            return Action.Reply(request, CastPreviewProtocol.STATUS_OK, it)
        }
        if (request.step in unavailable) return Action.Reply(request, CastPreviewProtocol.STATUS_NONE)
        return null
    }

    private fun switchTo(sourceId: String?) {
        frames.setSource(sourceId)
        frames.clear()
        unavailable.clear()
        failed = false
        inFlight = null
        waiting = null
    }
}
