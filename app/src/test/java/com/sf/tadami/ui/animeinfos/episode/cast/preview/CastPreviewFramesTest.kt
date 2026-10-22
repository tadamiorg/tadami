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
    fun `memory pressure drops the frames only`() {
        val frames = CastPreviewFrames()
        frames.onFetched(frames.onRequest(request(1)).fetched(), jpeg())
        frames.onFetched(frames.onRequest(request(2)).fetched(), null)
        frames.dropFrames()
        assertEquals(0, frames.size)
        assertEquals(SOURCE_A, frames.sourceId)
        frames.onRequest(request(1)).fetched()
        frames.onFetched(frames.inFlight!!, jpeg())
        assertEquals(CastPreviewProtocol.STATUS_NONE, frames.onRequest(request(2)).status())
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
