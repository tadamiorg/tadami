package com.sf.tadami.ui.animeinfos.episode.cast.preview

import com.sf.tadami.ui.animeinfos.episode.player.preview.PREVIEW_FRAME_CACHE_BYTES
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewFrameCache
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewFrameKey
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketOf
import kotlin.math.abs

/**
 * The phone-side frame cache of the cast session, and which decode runs next.
 *
 * Scoped to the ONE stream the TV plays (the receiver's sourceId, which changes with the episode and
 * the source): another stream drops every frame, and the session's end drops them all ([clear]). It
 * is never a media registry: it holds JPEG frames, keyed by preview step, nothing about how to reach
 * the stream (each request carries that).
 *
 * As in the phone player: frames are kept LRU within [PREVIEW_FRAME_CACHE_BYTES] (every step of an
 * episode fits); a step that gave no frame isn't decoded again; one decode at a time. A receiver's
 * request (a scrub) always comes before the background prefetch: it takes over a prefetch decode in
 * flight (or joins it, same step), and no prefetch starts while one waits. Main thread only.
 */
class CastPreviewFrames(maxBytes: Long = PREVIEW_FRAME_CACHE_BYTES) {

    sealed interface Action {
        /** Answer [request] with [status] (and [frame] when it's ok); pushed when it has no request id. */
        class Reply(val request: CastPreviewRequest, val status: String, val frame: PreviewJpeg? = null) : Action

        /** Decode the frame of [request]'s step. */
        class Fetch(val request: CastPreviewRequest) : Action
    }

    private val frames = PreviewFrameCache<PreviewJpeg>(maxBytes) { it.bytes.size.toLong() }

    // Steps that gave no frame in time: not decoded again for this stream
    private val unavailable = mutableSetOf<Int>()

    // The stream can't be decoded: every request about it is answered "off"
    private var failed = false

    // Frames dropped on memory pressure: the prefetch doesn't make them all again
    private var prefetchSuspended = false

    /** The decode running: a receiver's request, or a prefetch (no request id). */
    var inFlight: CastPreviewRequest? = null
        private set

    // A receiver's request waiting for the decode in flight to end
    private var waiting: CastPreviewRequest? = null

    // A receiver's request for the very step the prefetch in flight decodes
    private var joined: CastPreviewRequest? = null

    /** The stream whose frames are kept, null before the first request or after [clear]. */
    val sourceId: String?
        get() = frames.sourceId

    val size: Int
        get() = frames.size

    val bytes: Long
        get() = frames.bytes

    val unavailableCount: Int
        get() = unavailable.size

    /** No frame of the current stream will come any more (it can't be decoded). */
    val isFailed: Boolean
        get() = failed

    /** Makes [sourceId] the stream kept; another one than the current drops everything. True when it changed. */
    fun select(sourceId: String): Boolean {
        if (sourceId == frames.sourceId) return false
        switchTo(sourceId)
        return true
    }

    /** The step has its frame, or is known to have none. */
    fun isDone(step: Int): Boolean = PreviewFrameKey(frames.sourceId.orEmpty(), step) in frames || step in unavailable

    /**
     * A request from the receiver: its reply when it's known (cached frame, frameless step, stream
     * off), the decode to start, or null when it waits for the decode in flight.
     */
    fun onRequest(request: CastPreviewRequest): Action? {
        select(request.sourceId)
        known(request)?.let { return it }
        val current = inFlight
        if (current != null) {
            if (current.requestId != null) {
                waiting = request
                return null
            }
            // A prefetch: the scrub comes first
            if (current.step == request.step) {
                joined = request
                return null
            }
        }
        inFlight = request
        joined = null
        return Action.Fetch(request)
    }

    /** The prefetch of [request]'s step (no request id), unless a decode runs or waits, or it's done. */
    fun prefetch(request: CastPreviewRequest): Action.Fetch? {
        if (request.sourceId != frames.sourceId || inFlight != null || waiting != null) return null
        if (failed || prefetchSuspended || isDone(request.step)) return null
        inFlight = request
        return Action.Fetch(request)
    }

    /**
     * The decode of [request] ended with [frame], or none ([sourceFailed]: the stream can't be decoded
     * at all). Returns the reply (to the receiver's request that joined a prefetch, if any), then what
     * the waiting request needs; nothing when it was taken over or the stream changed meanwhile.
     */
    fun onFetched(request: CastPreviewRequest, frame: PreviewJpeg?, sourceFailed: Boolean = false): List<Action> {
        if (inFlight !== request) return emptyList()
        inFlight = null
        val answered = joined ?: request
        joined = null
        val actions = mutableListOf<Action>()
        actions += when {
            sourceFailed -> {
                failed = true
                Action.Reply(answered, CastPreviewProtocol.STATUS_OFF)
            }
            frame != null -> {
                frames.put(PreviewFrameKey(request.sourceId, request.step), frame)
                Action.Reply(answered, CastPreviewProtocol.STATUS_OK, frame)
            }
            else -> {
                unavailable += request.step
                Action.Reply(answered, CastPreviewProtocol.STATUS_NONE)
            }
        }
        waiting?.let { next ->
            waiting = null
            onRequest(next)?.let(actions::add)
        }
        return actions
    }

    /**
     * What is known of the current stream's steps, as pushes (no request id), nearest [centerStep]
     * first: for a receiver that lost them (restarted), but [except] those it holds.
     */
    fun knownPushes(template: CastPreviewSource, centerStep: Int, except: Set<Int>): List<Action.Reply> {
        if (template.sourceId != frames.sourceId) return emptyList()
        val lastStep = previewBucketOf(template.duration - 1, template.duration)
        val cached = (0..lastStep).filter { PreviewFrameKey(template.sourceId, it) in frames }
        return (cached + unavailable).distinct()
            .filter { it !in except }
            .sortedBy { abs(it - centerStep) }
            .map { step ->
                val request = template.requestFor(step)
                val frame = frames[PreviewFrameKey(template.sourceId, step)]
                if (frame != null) {
                    Action.Reply(request, CastPreviewProtocol.STATUS_OK, frame)
                } else {
                    Action.Reply(request, CastPreviewProtocol.STATUS_NONE)
                }
            }
    }

    /** The cast session ended: every frame dropped. */
    fun clear() {
        switchTo(null)
    }

    /** Memory pressure: the frames go, and the prefetch stops for this stream; what is known stays. */
    fun dropFrames() {
        frames.clear()
        prefetchSuspended = true
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
        prefetchSuspended = false
        inFlight = null
        waiting = null
        joined = null
    }
}
