package com.sf.tadami.ui.animeinfos.episode.cast.preview

import com.sf.tadami.ui.animeinfos.episode.cast.preview.CastPreviewPrefetch.Playback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** When and in which order the phone prefetches the cast's preview steps. */
class CastPreviewPrefetchTest {

    private var time = 1_000_000L
    private val prefetch = CastPreviewPrefetch { time }

    // 24 min: 14.4 s steps, 0..99
    private val duration = 24 * 60_000L

    private fun advance(ms: Long) {
        time += ms
    }

    /** Playing steadily at [positionMs]. */
    private fun playSteadily(positionMs: Long = 0L) {
        prefetch.onStatus(Playback.PLAYING, positionMs)
        advance(CastPreviewPrefetch.STABLE_MS)
        prefetch.onStatus(Playback.PLAYING, positionMs)
    }

    @Test
    fun `steps go from the center outward`() {
        assertEquals(listOf(5, 6, 4, 7, 3, 8, 2), prefetchOrder(5, 8).take(7).toList())
        assertEquals(listOf(0, 1, 2, 3), prefetchOrder(0, 3).toList())
        assertEquals(listOf(3, 2, 1, 0), prefetchOrder(3, 3).toList())
        assertEquals(listOf(3, 2, 1, 0), prefetchOrder(10, 3).toList())
        assertEquals(10, prefetchOrder(4, 9).toList().size)
        assertTrue(prefetchOrder(0, -1).toList().isEmpty())
    }

    @Test
    fun `the nearest step to the playback position first`() {
        playSteadily(positionMs = 10 * 14_400L + 5_000)
        assertEquals(10, prefetch.nextStep(duration) { false })
        val done = mutableSetOf(10, 11)
        assertEquals(9, prefetch.nextStep(duration) { it in done })
    }

    @Test
    fun `re-centred after a seek`() {
        playSteadily(positionMs = 0L)
        assertEquals(0, prefetch.nextStep(duration) { false })
        prefetch.onStatus(Playback.PLAYING, 60 * 14_400L)
        assertEquals(60, prefetch.nextStep(duration) { false })
    }

    @Test
    fun `stops once every step is done`() {
        playSteadily()
        assertNull(prefetch.nextStep(duration) { true })
        assertNull(prefetch.nextStep(0L) { false })
    }

    @Test
    fun `starts only once the cast has played steadily`() {
        assertFalse("nothing reported yet", prefetch.runnable())
        prefetch.onStatus(Playback.PLAYING, 0L)
        advance(CastPreviewPrefetch.STABLE_MS - 1)
        prefetch.onStatus(Playback.PLAYING, 0L)
        assertFalse(prefetch.runnable())
        advance(1)
        prefetch.onStatus(Playback.PLAYING, 0L)
        assertTrue(prefetch.runnable())
    }

    @Test
    fun `pauses while the TV buffers or loads, and waits for steady playback again`() {
        playSteadily()
        for (state in listOf(Playback.BUFFERING, Playback.LOADING, Playback.IDLE, null)) {
            prefetch.onStatus(state, 0L)
            assertFalse("$state", prefetch.runnable())
            prefetch.onStatus(Playback.PLAYING, 0L)
            assertFalse("$state, just playing again", prefetch.runnable())
            advance(CastPreviewPrefetch.STABLE_MS)
            prefetch.onStatus(Playback.PLAYING, 0L)
            assertTrue("$state, steady again", prefetch.runnable())
        }
    }

    @Test
    fun `goes on through a short pause, not a long one`() {
        playSteadily()
        prefetch.onStatus(Playback.PAUSED, 0L)
        assertTrue(prefetch.runnable())
        advance(CastPreviewPrefetch.LONG_PAUSE_MS - 1)
        prefetch.onStatus(Playback.PAUSED, 0L)
        assertTrue(prefetch.runnable())
        advance(1)
        prefetch.onStatus(Playback.PAUSED, 0L)
        assertFalse("paused for long", prefetch.runnable())
        // Playing again: steady at once, it was before the pause
        prefetch.onStatus(Playback.PLAYING, 0L)
        assertTrue(prefetch.runnable())
    }

    @Test
    fun `a never played cast doesn't prefetch while paused`() {
        prefetch.onStatus(Playback.PAUSED, 0L)
        advance(60_000)
        prefetch.onStatus(Playback.PAUSED, 0L)
        assertFalse(prefetch.runnable())
    }

    @Test
    fun `a gap after each decode, shorter for a download`() {
        playSteadily()
        assertTrue(prefetch.ready())
        prefetch.onDecodeEnded(local = false)
        assertFalse(prefetch.ready())
        advance(CastPreviewPrefetch.GAP_ONLINE_MS)
        assertTrue(prefetch.ready())
        prefetch.onDecodeEnded(local = true)
        advance(CastPreviewPrefetch.GAP_LOCAL_MS)
        assertTrue(prefetch.ready())
    }

    @Test
    fun `another stream awaits steady playback again`() {
        playSteadily()
        prefetch.onDecodeEnded(local = false)
        prefetch.reset()
        assertFalse(prefetch.runnable())
        assertTrue(prefetch.ready())
    }
}
