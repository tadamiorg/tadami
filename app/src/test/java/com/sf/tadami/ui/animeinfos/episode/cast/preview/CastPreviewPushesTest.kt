package com.sf.tadami.ui.animeinfos.episode.cast.preview

import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewSidecar
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketSizeMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** A downloaded episode's frames pushed to the receiver from its sidecar: which, in what order, how fast. */
class CastPreviewPushesTest {

    // 23:50.026: 14.3 s steps, 0..99 on the phone; the receiver has a step 100, 26 ms long
    private val duration = 1_430_026L
    private val url = "http://192.168.1.4:8214/local-0123456789abcdef0123456789abcdef-video.m3u8"
    private val sourceId = "7|$url|Downloaded|"

    private val sidecar = PreviewSidecar(
        videoLength = 42,
        durationMs = duration,
        stepMs = previewBucketSizeMs(duration),
        frameWidth = 256,
        frameHeight = 144,
        entries = (0..99).map { step ->
            if (step == 29) PreviewSidecar.Entry(step, -1, null) else PreviewSidecar.Entry(step, step * 14_300L, ByteArray(9_000) { step.toByte() })
        },
    )

    private fun source(durationMs: Long = duration, steps: List<Int> = emptyList()) = CastPreviewSource(
        type = CastPreviewProtocol.TYPE_SOURCE,
        sourceId = sourceId,
        url = url,
        duration = durationMs,
        steps = steps,
    )

    @Test
    fun `every receiver step gets the sidecar's frame of its time, as stored`() {
        val known = castSidecarFrames(sidecar, duration)!!
        assertEquals((0..100).toSet() - 29, known.frames.keys)
        assertEquals(setOf(29), known.none)
        assertSame(sidecar.entries[5].jpeg, known.frames.getValue(5).bytes)
        // The leftover end step shows the last frame
        assertSame(sidecar.entries[99].jpeg, known.frames.getValue(100).bytes)
        assertEquals(256 to 144, known.frames.getValue(0).width to known.frames.getValue(0).height)
        // The TV's duration a few hundred ms off: the same frames
        assertEquals((0..100).toSet() - 29, castSidecarFrames(sidecar, duration - 400)!!.frames.keys)
        // Another cut of the video: decoded on the phone instead
        assertNull(castSidecarFrames(sidecar, duration + 5_000))
    }

    @Test
    fun `a frame too big for a cast message is left to the decoder`() {
        val big = PreviewSidecar(42, duration, sidecar.stepMs, 256, 144, listOf(PreviewSidecar.Entry(0, 0, ByteArray(40_000))))
        val known = castSidecarFrames(big, duration)!!
        assertTrue(known.frames.isEmpty())
        assertTrue(known.none.isEmpty())
    }

    @Test
    fun `all of them are pushed at once, nearest the TV's position first, but those the receiver holds`() {
        val frames = CastPreviewFrames()
        frames.select(sourceId)
        frames.fill(sourceId, castSidecarFrames(sidecar, duration)!!)
        assertTrue((0..100).all(frames::isDone))
        val center = 40
        val pushes = frames.knownPushes(source(), center, except = setOf(41, 42))
        assertEquals((0..100).toList() - listOf(41, 42), pushes.map { it.request.step }.sorted())
        assertEquals(40, pushes.first().request.step)
        val distances = pushes.map { abs(it.request.step - center) }
        assertEquals("nearest first", distances.sorted(), distances)
        assertTrue("pushed, no request id", pushes.all { it.request.requestId == null })
        assertEquals(CastPreviewProtocol.STATUS_NONE, pushes.single { it.request.step == 29 }.status)
        // A receiver's scrub is answered from them at once
        val scrub = source().requestFor(77).copy(requestId = 9)
        val reply = frames.onRequest(scrub) as CastPreviewFrames.Action.Reply
        assertEquals(CastPreviewProtocol.STATUS_OK, reply.status)
        // Every pushed message fits a cast message
        assertTrue(pushes.all { (it.frame?.let { frame -> CastPreviewProtocol.frameMessage(it.request, frame) } ?: "x").length <= CastPreviewProtocol.MAX_MESSAGE_CHARS })
    }

    @Test
    fun `another stream's sidecar frames aren't taken`() {
        val frames = CastPreviewFrames()
        frames.select("another")
        frames.fill(sourceId, castSidecarFrames(sidecar, duration)!!)
        assertEquals(0, frames.size)
    }

    /** Pushes [count] frames, the channel confirming each [sentAfterMs] later (null: never): when the last went. */
    private fun pushAll(count: Int, sentAfterMs: Long?, pacer: PushPacer = PushPacer(CastSeekPreview.PUSH_GAP_MS, CastSeekPreview.PUSH_SENT_TIMEOUT_MS)): List<Long> {
        var now = 0L
        val sendTimes = mutableListOf<Long>()
        repeat(count) {
            now += pacer.delayAt(now)
            sendTimes += now
            pacer.onSending(now)
            if (sentAfterMs != null) {
                // Nothing else goes while it's in flight
                assertTrue(pacer.delayAt(now + sentAfterMs - 1) > 0)
                now += sentAfterMs
                pacer.onSent(now)
            }
        }
        return sendTimes
    }

    @Test
    fun `pushes go one at a time, as fast as the channel takes them`() {
        // A channel that takes each at once: 100 frames in about 1 s
        assertEquals(99 * 10L, pushAll(100, sentAfterMs = 0).last())
        // One taking 30 ms for each: about 4 s
        assertEquals(99 * 40L, pushAll(100, sentAfterMs = 30).last())
        // One that never says: one every half second
        assertEquals(99 * 500L, pushAll(100, sentAfterMs = null).last())
    }

    @Test
    fun `a new stream pushes at once`() {
        val pacer = PushPacer(gapMs = 20, sentTimeoutMs = 500)
        pacer.onSending(0)
        assertEquals(500L, pacer.delayAt(0))
        pacer.reset()
        assertEquals(0L, pacer.delayAt(0))
    }

    @Test
    fun `a scrub's request not answered at once is answered none, its frame pushed later`() {
        val replies = PendingReplies()
        val request = source().requestFor(12).copy(requestId = 5)
        replies.waiting(request)
        // Its frame came in time: the reply itself
        assertEquals(PendingReplies.Answer.REPLY, replies.onReply(request, CastPreviewProtocol.STATUS_OK))
        assertNull("answered already", replies.answerEarly(5))

        val slow = source().requestFor(13).copy(requestId = 6)
        replies.waiting(slow)
        assertSame(slow, replies.answerEarly(6))
        assertNull("once", replies.answerEarly(6))
        // The decode ends later: the frame is pushed, a step without one isn't sent again
        assertEquals(PendingReplies.Answer.PUSH, replies.onReply(slow, CastPreviewProtocol.STATUS_OK))
        val none = source().requestFor(14).copy(requestId = 7)
        replies.waiting(none)
        replies.answerEarly(7)
        assertEquals(PendingReplies.Answer.DROP, replies.onReply(none, CastPreviewProtocol.STATUS_NONE))
        // A pushed prefetch frame isn't a reply
        assertEquals(PendingReplies.Answer.REPLY, replies.onReply(source().requestFor(15), CastPreviewProtocol.STATUS_OK))
        // Another stream: nothing pending
        replies.waiting(source().requestFor(16).copy(requestId = 8))
        replies.clear()
        assertNull(replies.answerEarly(8))
    }
}
