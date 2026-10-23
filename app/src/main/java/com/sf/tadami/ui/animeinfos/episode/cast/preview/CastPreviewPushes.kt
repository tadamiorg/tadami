package com.sf.tadami.ui.animeinfos.episode.cast.preview

import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewSidecar
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketOf
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketRangeMs

/** A downloaded episode's sidecar frames as the receiver's steps: their JPEGs as stored, and the steps without. */
class CastSidecarFrames(val frames: Map<Int, PreviewJpeg>, val none: Set<Int>)

/**
 * The frames of [sidecar] for each step of a receiver whose duration is [durationMs] (its steps, see
 * [previewBucketOf]: a leftover end step included): the frame of its time, the JPEG as stored. Null when the
 * sidecar is another duration's (see [PreviewSidecar.matchesDuration]).
 */
fun castSidecarFrames(sidecar: PreviewSidecar, durationMs: Long): CastSidecarFrames? {
    if (!sidecar.matchesDuration(durationMs)) return null
    val frames = mutableMapOf<Int, PreviewJpeg>()
    val none = mutableSetOf<Int>()
    for (step in 0..previewBucketOf(durationMs - 1, durationMs)) {
        val entry = sidecar.entryAt(previewBucketRangeMs(step, durationMs)) ?: continue
        val jpeg = entry.jpeg
        // Within a cast message, as the cast's own (see CastPreviewProtocol.encodeWithinBudget)
        if (jpeg != null && CastPreviewProtocol.base64Length(jpeg.size) <= CastPreviewProtocol.MAX_FRAME_BASE64_CHARS) {
            frames[step] = PreviewJpeg(jpeg, sidecar.frameWidth, sidecar.frameHeight)
        } else if (jpeg == null) {
            none += step
        }
    }
    return CastSidecarFrames(frames, none)
}

/**
 * When the next pushed frame goes on the cast channel: one message in flight at a time, the next [gapMs] after
 * the channel said the previous one was sent, so the pushes go as fast as the channel takes them without piling
 * up in front of the playback's messages; [sentTimeoutMs] after a message the channel never confirmed. Pure: the
 * clock is given.
 */
class PushPacer(private val gapMs: Long, private val sentTimeoutMs: Long) {
    private var sendingSince: Long? = null
    private var sentAt: Long? = null

    /** How long until the next push may go at [now]: 0 when it may go now. */
    fun delayAt(now: Long): Long {
        sendingSince?.let { return (it + sentTimeoutMs - now).coerceAtLeast(0L) }
        return sentAt?.let { (it + gapMs - now).coerceAtLeast(0L) } ?: 0L
    }

    /** A push goes at [now]. */
    fun onSending(now: Long) {
        sendingSince = now
    }

    /** The channel sent it at [now]. */
    fun onSent(now: Long) {
        sendingSince = null
        sentAt = now
    }

    /** Another stream: nothing in flight. */
    fun reset() {
        sendingSince = null
        sentAt = null
    }
}

/**
 * The receiver's requests not answered at once: each gets an answer within [CAST_PREVIEW_REPLY_MS], "none" when
 * its frame isn't there by then (the receiver shows the time alone and moves on), and the frame comes later
 * as a push instead of the reply. The receiver asks one step at a time, so a quick answer keeps its scrub going.
 */
class PendingReplies {
    /** What to do with a reply that is ready. */
    enum class Answer {
        /** Send it, the request's own reply */
        REPLY,

        /** Answered "none" already: its frame goes as a push (no request id) */
        PUSH,

        /** Answered "none" already, and it has none: nothing to send */
        DROP,
    }

    private val waiting = mutableMapOf<Long, CastPreviewRequest>()
    private val answeredEarly = mutableSetOf<Long>()

    /** [request] waits for a decode (or the frames kept next to the episode). */
    fun waiting(request: CastPreviewRequest) {
        request.requestId?.let { waiting[it] = request }
    }

    /** The request [requestId] gets its "none" now, unless it was answered meanwhile. */
    fun answerEarly(requestId: Long): CastPreviewRequest? {
        val request = waiting.remove(requestId) ?: return null
        answeredEarly += requestId
        return request
    }

    /** A reply with [status] is ready for [request]. */
    fun onReply(request: CastPreviewRequest, status: String): Answer {
        val id = request.requestId ?: return Answer.REPLY
        waiting.remove(id)
        if (!answeredEarly.remove(id)) return Answer.REPLY
        return if (status == CastPreviewProtocol.STATUS_NONE) Answer.DROP else Answer.PUSH
    }

    fun clear() {
        waiting.clear()
        answeredEarly.clear()
    }
}

/** How long a receiver's request waits for its frame before it is answered "none" (the frame then pushed). */
const val CAST_PREVIEW_REPLY_MS = 300L
