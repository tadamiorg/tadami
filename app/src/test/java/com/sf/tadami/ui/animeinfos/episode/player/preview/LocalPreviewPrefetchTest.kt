package com.sf.tadami.ui.animeinfos.episode.player.preview

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewPrefetch.Playback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The phone player's background prefetch of a downloaded episode's preview frames: its order, its
 * pauses, the scrubs' priority and the frames fitting the cache.
 */
class LocalPreviewPrefetchTest {

    private var time = 1_000_000L
    private val schedule = phonePreviewPrefetch { time }
    private val requests = BucketRequests()
    private val done = mutableSetOf<Int>()

    // 23:50, a downloaded episode: 14.3 s steps, 0..99
    private val duration = 1_430_000L

    private fun next(hasRoom: Boolean = true) = nextPreviewPrefetch(schedule, requests, duration, { it in done }, hasRoom)

    private fun playSteadily(positionMs: Long) {
        schedule.onStatus(Playback.PLAYING, positionMs)
        time += PreviewPrefetch.STABLE_MS
        schedule.onStatus(Playback.PLAYING, positionMs)
    }

    /** Starts the prefetch [next] picks, then completes it with a frame and lets the gap pass. */
    private fun prefetchNext(): Int {
        val step = (next() as PreviewPrefetchStep.Fetch).bucket
        assertTrue(requests.prefetch(step))
        done += step
        requests.finished(step)
        schedule.onDecodeEnded(500)
        time += 500
        return step
    }

    @Test
    fun `the setting and the seekbar preview turn the prefetch on and off, for a downloaded episode only`() {
        assertTrue(phonePreviewPrefetchOn(seekPreview = true, prefetch = true, downloaded = true))
        assertFalse("prefetch off", phonePreviewPrefetchOn(seekPreview = true, prefetch = false, downloaded = true))
        assertFalse("preview off", phonePreviewPrefetchOn(seekPreview = false, prefetch = true, downloaded = true))
        assertFalse("a stream, or nothing playing", phonePreviewPrefetchOn(seekPreview = true, prefetch = true, downloaded = false))
    }

    @Test
    fun `one setting for the phone and the cast, its stored choice kept`() {
        assertTrue(PlayerPreferences.transform(emptyPreferences()).seekPreviewPrefetch)
        // Stored under the key of the cast's former setting
        val stored = mutablePreferencesOf(booleanPreferencesKey("player_seek_preview_cast_prefetch") to false)
        assertFalse(PlayerPreferences.transform(stored).seekPreviewPrefetch)
    }

    @Test
    fun `nearest the playback position first, then outward`() {
        playSteadily(positionMs = 40 * 14_300L)
        assertEquals(listOf(40, 41, 39, 42, 38), List(5) { prefetchNext() })
    }

    @Test
    fun `re-centred after a seek of the main player`() {
        playSteadily(positionMs = 0L)
        assertEquals(listOf(0, 1), List(2) { prefetchNext() })
        schedule.onStatus(Playback.PLAYING, 80 * 14_300L)
        assertEquals(listOf(80, 81, 79), List(3) { prefetchNext() })
    }

    @Test
    fun `starts after one second of playback`() {
        schedule.onStatus(Playback.PLAYING, 0L)
        time += 999
        schedule.onStatus(Playback.PLAYING, 0L)
        assertEquals(PreviewPrefetchStep.Wait, next())
        time += 1
        schedule.onStatus(Playback.PLAYING, 0L)
        assertEquals(PreviewPrefetchStep.Fetch(0), next())
        // A buffering main player starts the second again
        schedule.onStatus(Playback.BUFFERING, 0L)
        schedule.onStatus(Playback.PLAYING, 0L)
        time += 999
        schedule.onStatus(Playback.PLAYING, 0L)
        assertEquals(PreviewPrefetchStep.Wait, next())
    }

    @Test
    fun `waits for steady playback, and pauses while the main player buffers`() {
        assertEquals(PreviewPrefetchStep.Wait, next())
        playSteadily(positionMs = 0L)
        prefetchNext()
        schedule.onStatus(Playback.BUFFERING, 0L)
        assertEquals(PreviewPrefetchStep.Wait, next())
        // The screen in the background: no playback reported
        playSteadily(positionMs = 0L)
        schedule.onStatus(null, 0L)
        assertEquals(PreviewPrefetchStep.Wait, next())
        playSteadily(positionMs = 0L)
        assertEquals(PreviewPrefetchStep.Fetch(1), next())
    }

    @Test
    fun `goes on while paused, not after a long pause`() {
        playSteadily(positionMs = 0L)
        schedule.onStatus(Playback.PAUSED, 0L)
        assertEquals(PreviewPrefetchStep.Fetch(0), next())
        time += PreviewPrefetch.LONG_PAUSE_MS
        schedule.onStatus(Playback.PAUSED, 0L)
        assertEquals(PreviewPrefetchStep.Wait, next())
    }

    @Test
    fun `one fetch at a time, with a gap after each prefetch`() {
        playSteadily(positionMs = 0L)
        val first = (next() as PreviewPrefetchStep.Fetch).bucket
        assertTrue(requests.prefetch(first))
        assertEquals("loading", PreviewPrefetchStep.Wait, next())
        done += first
        requests.finished(first)
        schedule.onDecodeEnded(500)
        assertEquals("the gap", PreviewPrefetchStep.Wait, next())
        time += 500
        assertEquals(PreviewPrefetchStep.Fetch(1), next())
    }

    @Test
    fun `a scrub comes first, then the prefetch goes on`() {
        playSteadily(positionMs = 0L)
        val prefetching = (next() as PreviewPrefetchStep.Fetch).bucket
        assertTrue(requests.prefetch(prefetching))

        // A finger on the seekbar over another step: it takes over the prefetch
        requests.aim(60)
        assertEquals(60, requests.next { it in done })
        assertFalse(requests.inFlightIsPrefetch)
        // While the finger is there, nothing is prefetched
        done += 60
        requests.finished(60)
        assertEquals(PreviewPrefetchStep.Wait, next())
        assertFalse(requests.prefetch(1))
        // The drag ended: the prefetch starts again, the step it dropped included
        requests.aim(null)
        assertEquals(PreviewPrefetchStep.Fetch(prefetching), next())
    }

    @Test
    fun `a scrub over the step the prefetch loads waits for it`() {
        playSteadily(positionMs = 0L)
        assertTrue(requests.prefetch(0))
        requests.aim(0)
        assertNull(requests.next { it in done })
        assertEquals(0, requests.inFlight)
        assertTrue(requests.inFlightIsPrefetch)
    }

    @Test
    fun `a scrub's own fetch isn't taken over by another scrub's step`() {
        requests.aim(10)
        assertEquals(10, requests.next { false })
        requests.aim(20)
        assertNull(requests.next { false })
        assertFalse(requests.prefetch(30))
    }

    @Test
    fun `never prefetches a leftover end step, merged into the one before it`() {
        // 23:50.026: steps 0..99, step 100 being 26 ms long
        val download = 1_430_026L
        playSteadily(positionMs = download - 1)
        assertEquals(PreviewPrefetchStep.Fetch(99), nextPreviewPrefetch(schedule, requests, download, { it in done }, true))
        done += 0..99
        assertEquals(PreviewPrefetchStep.Done, nextPreviewPrefetch(schedule, requests, download, { it in done }, true))
        // The cast's own picks keep every step
        assertEquals(100, schedule.nextStep(download) { it in done })
    }

    @Test
    fun `a new file's duration picks the step at the position`() {
        // 10:00 into a 23:50.026 episode: step 41 of 14.3 s steps; into a 48:00 one: step 20 of 28.8 s steps
        playSteadily(positionMs = 600_000L)
        assertEquals(PreviewPrefetchStep.Fetch(41), nextPreviewPrefetch(schedule, requests, 1_430_026L, { false }, true))
        assertEquals(PreviewPrefetchStep.Fetch(20), nextPreviewPrefetch(schedule, requests, 2_880_000L, { false }, true))
    }

    @Test
    fun `stops once every step is done`() {
        playSteadily(positionMs = 0L)
        done += 0..99
        assertEquals(PreviewPrefetchStep.Done, next())
    }

    @Test
    fun `stops rather than evict when the cache is full`() {
        playSteadily(positionMs = 0L)
        assertEquals(PreviewPrefetchStep.Done, next(hasRoom = false))
    }

    @Test
    fun `every step of a film fits the cache as RGB_565 frames`() {
        // Steps are 30 s at most: 2.5 h is 300 steps
        val filmMs = 150 * 60_000L
        val steps = previewBucketOf(filmMs - 1, filmMs) + 1
        assertEquals(300, steps)
        val cache = PreviewFrameCache<Long>(PREVIEW_BITMAP_CACHE_BYTES) { it }.apply { setSource("film") }
        repeat(steps) { step ->
            assertTrue("step $step", cache.hasRoomFor(PREVIEW_FRAME_BYTES))
            cache.put(PreviewFrameKey("film", step), PREVIEW_FRAME_BYTES)
        }
        assertEquals("nothing evicted", 300, cache.size)
        // 341 frames in all: 2 h 50
        assertEquals(341L, PREVIEW_BITMAP_CACHE_BYTES / PREVIEW_FRAME_BYTES)
        // As ARGB frames (twice the bytes) a film wouldn't fit
        assertTrue(PREVIEW_BITMAP_CACHE_BYTES / (PREVIEW_FRAME_BYTES * 2) < 300)
    }

    @Test
    fun `an older download's prefetched frames are saved next to it once every step is done`() {
        fun save(
            downloaded: Boolean = true,
            loadedFromSidecar: Boolean = false,
            saved: Boolean = false,
            everyStepDone: Boolean = true,
        ) = shouldSavePrefetchedPreviews(downloaded, loadedFromSidecar, saved, everyStepDone)

        assertTrue("no frames kept next to it", save())
        assertFalse("its frames came from its sidecar", save(loadedFromSidecar = true))
        assertFalse("saved already", save(saved = true))
        assertFalse("the cache filled up first", save(everyStepDone = false))
        assertFalse("nowhere to keep them", save(downloaded = false))
    }

    @Test
    fun `the prefetch's done once the sidecar's frames fill every step, saving nothing`() {
        playSteadily(positionMs = 0L)
        // The sidecar's frames, put in the cache as the episode opens
        done += 0..99
        assertEquals(PreviewPrefetchStep.Done, next())
        assertFalse(shouldSavePrefetchedPreviews(downloaded = true, loadedFromSidecar = true, saved = false, everyStepDone = true))
    }

    @Test
    fun `the cache tells when a frame would evict another`() {
        val cache = PreviewFrameCache<Long>(300) { it }.apply { setSource("a") }
        cache.put(PreviewFrameKey("a", 0), 200)
        assertTrue(cache.hasRoomFor(100))
        assertFalse(cache.hasRoomFor(101))
    }
}
