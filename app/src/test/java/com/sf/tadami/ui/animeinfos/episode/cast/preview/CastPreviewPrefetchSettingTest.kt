package com.sf.tadami.ui.animeinfos.episode.cast.preview

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewPrefetch
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewPrefetch.Playback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** The "Prefetch preview frames when casting" setting: it gates the background prefetch only, live. */
class CastPreviewPrefetchSettingTest {

    private var time = 1_000_000L
    private val prefetch = PreviewPrefetch { time }
    private val frames = CastPreviewFrames()

    // 60 s: 5 s steps, 0..11
    private val source = CastPreviewSource(
        type = CastPreviewProtocol.TYPE_SOURCE,
        sourceId = "7|https://h.example/ep.m3u8|S|1080p",
        url = "https://h.example/ep.m3u8",
        duration = 60_000,
    )

    private fun jpeg() = PreviewJpeg(ByteArray(1_000), 256, 144)

    private fun playSteadily(positionMs: Long) {
        prefetch.onStatus(Playback.PLAYING, positionMs)
        time += PreviewPrefetch.STABLE_MS
        prefetch.onStatus(Playback.PLAYING, positionMs)
    }

    private fun tick(prefetchOn: Boolean, receiverHas: Set<Int> = emptySet()) =
        nextPrefetch(source, prefetch, frames, allowed = prefetchOn, receiverHas = receiverHas)

    /** Decodes what [decision] starts, then lets the gap pass. */
    private fun decode(decision: PrefetchDecision): Int {
        assertTrue("expected a decode, got $decision", decision is PrefetchDecision.Start)
        val request = (decision as PrefetchDecision.Start).fetch.request
        frames.onFetched(request, jpeg())
        prefetch.onDecodeEnded(CAST_PREFETCH_GAP_ONLINE_MS)
        time += CAST_PREFETCH_GAP_ONLINE_MS
        return request.step
    }

    @Test
    fun `the setting is on by default and stored`() {
        assertTrue(PlayerPreferences.transform(emptyPreferences()).seekPreviewCastPrefetch)
        assertTrue(CastSeekPreview.Settings().prefetch)
        val off = PlayerPreferences.transform(emptyPreferences()).copy(seekPreviewCastPrefetch = false)
        val stored = mutablePreferencesOf().also { PlayerPreferences.setPrefs(off, it) }
        assertFalse(PlayerPreferences.transform(stored).seekPreviewCastPrefetch)
    }

    @Test
    fun `off, nothing is prefetched, but a scrub is still decoded and answered`() {
        frames.select(source.sourceId)
        playSteadily(positionMs = 20_000)
        assertEquals(PrefetchDecision.Wait, tick(prefetchOn = false))
        assertNull(frames.inFlight)

        val scrub = CastPreviewRequest(
            type = CastPreviewProtocol.TYPE_REQUEST,
            requestId = 1,
            sourceId = source.sourceId,
            url = source.url,
            step = 9,
            duration = source.duration,
        )
        val fetch = frames.onRequest(scrub) as CastPreviewFrames.Action.Fetch
        assertSame(scrub, fetch.request)
        val reply = frames.onFetched(scrub, jpeg()).single() as CastPreviewFrames.Action.Reply
        assertEquals(CastPreviewProtocol.STATUS_OK, reply.status)
        assertEquals(1L, reply.request.requestId)
        // Still nothing in the background
        assertEquals(PrefetchDecision.Wait, tick(prefetchOn = false))
    }

    @Test
    fun `switched on and off during a cast, the prefetch starts and stops without reconnecting`() {
        frames.select(source.sourceId)
        playSteadily(positionMs = 20_000)
        assertEquals(PrefetchDecision.Wait, tick(prefetchOn = false))

        // On: from the playback position
        assertEquals(4, decode(tick(prefetchOn = true)))
        assertEquals(5, decode(tick(prefetchOn = true)))

        // Off: steps remain, none is started; the frames made stay for the scrubs
        assertEquals(PrefetchDecision.Wait, tick(prefetchOn = false))
        assertEquals(PrefetchDecision.Wait, tick(prefetchOn = false))
        assertEquals(2, frames.size)

        // On again: it goes on where it stopped
        assertEquals(3, decode(tick(prefetchOn = true)))
    }

    @Test
    fun `the playback gating still applies with the setting on`() {
        frames.select(source.sourceId)
        assertEquals("not played yet", PrefetchDecision.Wait, tick(prefetchOn = true))
        playSteadily(positionMs = 0)
        prefetch.onStatus(Playback.BUFFERING, 0)
        assertEquals("buffering", PrefetchDecision.Wait, tick(prefetchOn = true))
        playSteadily(positionMs = 0)
        prefetch.onStatus(Playback.PAUSED, 0)
        time += PreviewPrefetch.LONG_PAUSE_MS
        prefetch.onStatus(Playback.PAUSED, 0)
        assertEquals("paused for long", PrefetchDecision.Wait, tick(prefetchOn = true))
    }

    @Test
    fun `complete once every step is done or held by the receiver`() {
        frames.select(source.sourceId)
        playSteadily(positionMs = 0)
        val held = (0..9).toSet()
        assertEquals(10, decode(tick(prefetchOn = true, receiverHas = held)))
        assertEquals(11, decode(tick(prefetchOn = true, receiverHas = held)))
        assertEquals(PrefetchDecision.Complete, tick(prefetchOn = true, receiverHas = held))
    }
}
