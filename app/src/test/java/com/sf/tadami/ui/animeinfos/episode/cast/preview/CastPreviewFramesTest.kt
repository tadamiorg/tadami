package com.sf.tadami.ui.animeinfos.episode.cast.preview

import com.sf.tadami.ui.animeinfos.episode.cast.preview.CastPreviewFrames.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** The phone-side frame cache of a cast session: its scope (one stream), its clearing, one decode at a time. */
class CastPreviewFramesTest {

    private var nextId = 1L

    private fun request(step: Int, sourceId: String = SOURCE_A) = CastPreviewRequest(
        type = CastPreviewProtocol.TYPE_REQUEST,
        requestId = nextId++,
        sourceId = sourceId,
        url = "https://h.example/$sourceId.m3u8",
        step = step,
        duration = 1_420_000,
    )

    private fun jpeg(size: Int = 10_000) = PreviewJpeg(ByteArray(size), 256, 144)

    private fun Action?.fetched(): CastPreviewRequest {
        assertTrue("expected a fetch, got $this", this is Action.Fetch)
        return (this as Action.Fetch).request
    }

    private fun Action?.status(): String {
        assertTrue("expected a reply, got $this", this is Action.Reply)
        return (this as Action.Reply).status
    }

    @Test
    fun `decodes a step once and answers it from the cache after`() {
        val frames = CastPreviewFrames()
        val first = frames.onRequest(request(3)).fetched()
        val frame = jpeg()
        val replies = frames.onFetched(first, frame)
        assertEquals(1, replies.size)
        assertEquals(CastPreviewProtocol.STATUS_OK, replies[0].status())
        assertSame(frame, (replies[0] as Action.Reply).frame)

        // Asked again (the receiver lost the reply, or another session tab): no new decode
        val again = frames.onRequest(request(3))
        assertEquals(CastPreviewProtocol.STATUS_OK, again.status())
        assertSame(frame, (again as Action.Reply).frame)
        assertEquals(1, frames.size)
    }

    @Test
    fun `a step that gave no frame isn't decoded again`() {
        val frames = CastPreviewFrames()
        val first = frames.onRequest(request(5)).fetched()
        assertEquals(CastPreviewProtocol.STATUS_NONE, frames.onFetched(first, null).single().status())
        assertEquals(CastPreviewProtocol.STATUS_NONE, frames.onRequest(request(5)).status())
    }

    @Test
    fun `a stream that can't be decoded is off until another one`() {
        val frames = CastPreviewFrames()
        val first = frames.onRequest(request(1)).fetched()
        assertEquals(CastPreviewProtocol.STATUS_OFF, frames.onFetched(first, null, sourceFailed = true).single().status())
        assertEquals(CastPreviewProtocol.STATUS_OFF, frames.onRequest(request(2)).status())
        frames.onRequest(request(2, SOURCE_B)).fetched()
    }

    @Test
    fun `one decode at a time, the latest waiting request next`() {
        val frames = CastPreviewFrames()
        val first = frames.onRequest(request(1)).fetched()
        assertNull(frames.onRequest(request(2)))
        val latest = request(3)
        assertNull(frames.onRequest(latest))

        val actions = frames.onFetched(first, jpeg())
        assertEquals(2, actions.size)
        assertEquals(CastPreviewProtocol.STATUS_OK, actions[0].status())
        assertSame(latest, actions[1].fetched())
        assertSame(latest, frames.inFlight)
    }

    @Test
    fun `a waiting request for a step just decoded is answered from the cache`() {
        val frames = CastPreviewFrames()
        val first = frames.onRequest(request(4)).fetched()
        assertNull(frames.onRequest(request(4)))
        val actions = frames.onFetched(first, jpeg())
        assertEquals(listOf(CastPreviewProtocol.STATUS_OK, CastPreviewProtocol.STATUS_OK), actions.map { it.status() })
        assertNull(frames.inFlight)
    }

    @Test
    fun `another stream drops every frame and the decode in flight`() {
        val frames = CastPreviewFrames()
        frames.onFetched(frames.onRequest(request(1)).fetched(), jpeg())
        frames.onFetched(frames.onRequest(request(2)).fetched(), null)
        val inFlight = frames.onRequest(request(3)).fetched()

        // New episode or source: its own frames only
        val other = frames.onRequest(request(1, SOURCE_B)).fetched()
        assertEquals(SOURCE_B, frames.sourceId)
        assertEquals(0, frames.size)
        // The old stream's decode lands too late: dropped, not cached, not answered
        assertTrue(frames.onFetched(inFlight, jpeg()).isEmpty())
        assertEquals(0, frames.size)
        assertSame(other, frames.inFlight)

        // Back to the first stream: decoded again from scratch
        frames.onFetched(other, jpeg())
        frames.onRequest(request(1)).fetched()
        assertEquals(0, frames.size)
    }

    @Test
    fun `the end of the session clears everything`() {
        val frames = CastPreviewFrames()
        frames.onFetched(frames.onRequest(request(1)).fetched(), jpeg())
        val inFlight = frames.onRequest(request(2)).fetched()
        frames.clear()
        assertNull(frames.sourceId)
        assertEquals(0, frames.size)
        assertEquals(0L, frames.bytes)
        assertNull(frames.inFlight)
        assertTrue(frames.onFetched(inFlight, jpeg()).isEmpty())
    }

    @Test
    fun `memory pressure drops the frames, and the prefetch stops`() {
        val frames = CastPreviewFrames()
        frames.onFetched(frames.onRequest(request(1)).fetched(), jpeg())
        frames.onFetched(frames.onRequest(request(2)).fetched(), null)
        frames.dropFrames()
        assertEquals(0, frames.size)
        assertEquals(SOURCE_A, frames.sourceId)
        assertNull("no prefetch remaking them all", frames.prefetch(source().requestFor(5)))
        // A scrub still gets its frame
        frames.onRequest(request(1)).fetched()
        frames.onFetched(frames.inFlight!!, jpeg())
        assertEquals(CastPreviewProtocol.STATUS_NONE, frames.onRequest(request(2)).status())
    }

    // --- Prefetch ---

    private fun source(sourceId: String = SOURCE_A) = CastPreviewSource(
        type = CastPreviewProtocol.TYPE_SOURCE,
        sourceId = sourceId,
        url = "https://h.example/$sourceId.m3u8",
        duration = 1_420_000,
    )

    @Test
    fun `a prefetch decodes a step and pushes it, without request id`() {
        val frames = CastPreviewFrames()
        frames.select(SOURCE_A)
        val prefetch = frames.prefetch(source().requestFor(7))!!.request
        assertNull(prefetch.requestId)
        val reply = frames.onFetched(prefetch, jpeg()).single() as Action.Reply
        assertNull(reply.request.requestId)
        assertEquals(CastPreviewProtocol.STATUS_OK, reply.status)
        assertTrue(frames.isDone(7))
        assertNull("done: not again", frames.prefetch(source().requestFor(7)))
    }

    @Test
    fun `no prefetch while a scrub's request decodes or waits, nor for another stream`() {
        val frames = CastPreviewFrames()
        val scrub = frames.onRequest(request(1)).fetched()
        assertNull(frames.prefetch(source().requestFor(2)))
        frames.onFetched(scrub, jpeg())
        val prefetch = frames.prefetch(source().requestFor(2))!!.request
        // A scrub takes over; the prefetch can't start again meanwhile
        frames.onRequest(request(3)).fetched()
        assertNull(frames.prefetch(source().requestFor(4)))
        assertTrue("the taken-over prefetch is dropped", frames.onFetched(prefetch, jpeg()).isEmpty())
        assertNull(frames.prefetch(source(SOURCE_B).requestFor(4)))
    }

    @Test
    fun `a scrub takes over the prefetch in flight`() {
        val frames = CastPreviewFrames()
        frames.select(SOURCE_A)
        val prefetch = frames.prefetch(source().requestFor(10))!!.request
        val scrub = frames.onRequest(request(40)).fetched()
        assertSame(scrub, frames.inFlight)
        // The prefetch's late result is ignored, its step stays to do
        assertTrue(frames.onFetched(prefetch, jpeg()).isEmpty())
        assertTrue(!frames.isDone(10))
        assertEquals(CastPreviewProtocol.STATUS_OK, frames.onFetched(scrub, jpeg()).single().status())
    }

    @Test
    fun `a scrub on the step the prefetch decodes joins it, answered once`() {
        val frames = CastPreviewFrames()
        frames.select(SOURCE_A)
        val prefetch = frames.prefetch(source().requestFor(10))!!.request
        val scrub = request(10)
        assertNull(frames.onRequest(scrub))
        val reply = frames.onFetched(prefetch, jpeg()).single() as Action.Reply
        // The receiver's request is answered (with its id), no separate push
        assertSame(scrub, reply.request)
    }

    @Test
    fun `no prefetch of a stream that can't be decoded`() {
        val frames = CastPreviewFrames()
        frames.select(SOURCE_A)
        frames.onFetched(frames.prefetch(source().requestFor(1))!!.request, null, sourceFailed = true)
        assertTrue(frames.isFailed)
        assertNull(frames.prefetch(source().requestFor(2)))
    }

    @Test
    fun `what is known goes again nearest the position first, but what the receiver holds`() {
        val frames = CastPreviewFrames()
        frames.select(SOURCE_A)
        listOf(0, 5, 9, 10, 12).forEach { step ->
            val fetched = frames.prefetch(source().requestFor(step))!!.request
            frames.onFetched(fetched, if (step == 9) null else jpeg())
        }
        val pushes = frames.knownPushes(source(), centerStep = 10, except = setOf(12))
        assertEquals(listOf(10, 9, 5, 0), pushes.map { it.request.step })
        assertEquals(CastPreviewProtocol.STATUS_NONE, pushes[1].status)
        assertTrue(pushes.all { it.request.requestId == null })
        assertTrue(frames.knownPushes(source(SOURCE_B), 10, emptySet()).isEmpty())
    }

    @Test
    fun `frames are kept within the byte budget, least recently used first out`() {
        val frames = CastPreviewFrames(maxBytes = 25_000)
        (0..2).forEach { step -> frames.onFetched(frames.onRequest(request(step)).fetched(), jpeg()) }
        assertEquals(2, frames.size)
        assertEquals(20_000L, frames.bytes)
        // Step 0 went first: decoded again
        frames.onRequest(request(0)).fetched()
    }

    private companion object {
        const val SOURCE_A = "7|https://a.example/ep.m3u8|A|1080p"
        const val SOURCE_B = "7|https://b.example/ep.mp4|B|720p"
    }
}
