package com.sf.tadami.ui.animeinfos.episode.player.preview

import android.content.ComponentCallbacks2
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekPreviewLogicTest {

    @Test
    fun `seek fraction is the position over the duration, clamped`() {
        assertEquals(0.25f, seekFraction(positionMs = 300_000, durationMs = 1_200_000), 1e-6f)
        assertEquals(1f, seekFraction(positionMs = 1_300_000, durationMs = 1_200_000), 0f)
        assertEquals(0f, seekFraction(positionMs = -5, durationMs = 1_200_000), 0f)
    }

    @Test
    fun `seek fraction is 0 while the duration is unknown`() {
        assertEquals(0f, seekFraction(positionMs = 5_000, durationMs = 0), 0f)
        assertEquals(0f, seekFraction(positionMs = 5_000, durationMs = -1), 0f)
    }

    // A 1000 px slider from x = 100, 20 px thumb, 200 px preview, screen bounds 0..1300
    private fun left(fraction: Float, boundsStart: Float = 0f, boundsEnd: Float = 1300f) = seekPreviewLeft(
        fraction = fraction,
        sliderStart = 100f,
        sliderWidth = 1000f,
        thumbWidth = 20f,
        previewWidth = 200f,
        boundsStart = boundsStart,
        boundsEnd = boundsEnd,
    )

    @Test
    fun `preview is centred on the thumb`() {
        // Thumb centre: 100 + 10 + 0.5 * 980 = 600
        assertEquals(500f, left(0.5f), 1e-3f)
        // Thumb centre: 100 + 10 + 0.25 * 980 = 355
        assertEquals(255f, left(0.25f), 1e-3f)
    }

    @Test
    fun `preview is kept within the screen edges`() {
        // Thumb centre 110 would put the left edge at 10, before the 50 px bound
        assertEquals(50f, left(0f, boundsStart = 50f), 0f)
        // Thumb centre 1090 would put the right edge at 1190, past the 1150 px bound
        assertEquals(950f, left(1f, boundsEnd = 1150f), 0f)
        // Out of range fractions are clamped to the slider ends
        assertEquals(left(1f), left(1.5f), 0f)
    }

    @Test
    fun `preview wider than the bounds sticks to the start bound`() {
        assertEquals(40f, left(0.5f, boundsStart = 40f, boundsEnd = 200f), 0f)
    }

    @Test
    fun `preview tracks are the lowest bitrate video only`() {
        val parameters = previewTrackSelectionParameters(TrackSelectionParameters.DEFAULT)
        assertTrue(parameters.forceLowestBitrate)
        assertTrue(C.TRACK_TYPE_AUDIO in parameters.disabledTrackTypes)
        assertTrue(C.TRACK_TYPE_TEXT in parameters.disabledTrackTypes)
        assertFalse(C.TRACK_TYPE_VIDEO in parameters.disabledTrackTypes)
    }

    private val episode = 1_440_000L // 24 min

    @Test
    fun `steps are 1 percent of the duration within 5 and 30 s`() {
        assertEquals(14_400L, previewBucketSizeMs(episode))
        // 3 min: 1% would be 1.8 s
        assertEquals(5_000L, previewBucketSizeMs(180_000))
        // 2 h: 1% would be 72 s
        assertEquals(30_000L, previewBucketSizeMs(7_200_000))
        // Unknown duration
        assertEquals(5_000L, previewBucketSizeMs(0))
    }

    @Test
    fun `positions map to their step, ends included`() {
        assertEquals(0, previewBucketOf(0, episode))
        assertEquals(0, previewBucketOf(14_399, episode))
        assertEquals(1, previewBucketOf(14_400, episode))
        assertEquals(99, previewBucketOf(episode - 1, episode))
        // The end and past it belong to the last step, before the start to the first
        assertEquals(99, previewBucketOf(episode, episode))
        assertEquals(99, previewBucketOf(episode + 60_000, episode))
        assertEquals(0, previewBucketOf(-1_000, episode))
    }

    @Test
    fun `short and long videos get sensible step counts`() {
        // 3 min in 5 s steps: 36 steps, the last one 0..35
        assertEquals(35, previewBucketOf(179_999, 180_000))
        // 2 h in 30 s steps
        assertEquals(239, previewBucketOf(7_199_999, 7_200_000))
        // A 7 s clip: two steps, the second one shorter
        assertEquals(5_000L until 7_000L, previewBucketRangeMs(1, 7_000))
    }

    @Test
    fun `the phone merges a leftover end step into the one before it`() {
        // 23:50.026 in 14.3 s steps: step 100 is 26 ms long
        val download = 1_430_026L
        assertEquals(100, previewBucketOf(download - 1, download))
        assertEquals(99, phonePreviewBucketOf(1_429_999, download))
        assertEquals(99, phonePreviewBucketOf(1_430_000, download))
        assertEquals(99, phonePreviewBucketOf(1_430_025, download))
        assertEquals(99, phonePreviewBucketOf(download + 5_000, download))
        assertEquals(98, phonePreviewBucketOf(1_415_699, download))
        // 23:40.050 in 14.2 s steps: a 50 ms step 100
        assertEquals(99, phonePreviewBucketOf(1_420_049, 1_420_050))
        // 5 s steps of a 5:01 to 5:03 video: a 1 or 2 s leftover is merged, a 3 s one (half a step or more) kept
        assertEquals(59, phonePreviewBucketOf(300_500, 301_000))
        assertEquals(59, phonePreviewBucketOf(301_500, 302_000))
        assertEquals(60, phonePreviewBucketOf(302_500, 303_000))
        // A 7 s clip: its 2 s second step too
        assertEquals(0, phonePreviewBucketOf(6_000, 7_000))
    }

    @Test
    fun `the phone keeps a full or long enough end step`() {
        // 24:00 in 14.4 s steps: step 99 is a full one
        assertEquals(99, phonePreviewBucketOf(episode - 1, episode))
        assertEquals(previewBucketOf(1_430_000, episode), phonePreviewBucketOf(1_430_000, episode))
        // Exactly half a step left: kept
        assertEquals(60, phonePreviewBucketOf(302_499, 302_500))
    }

    @Test
    fun `the phone's steps of an unknown or tiny duration are the shared ones`() {
        assertEquals(0, phonePreviewBucketOf(5_000, 0))
        assertEquals(0, phonePreviewBucketOf(5_000, -1))
        assertEquals(0, phonePreviewBucketOf(0, 1))
    }

    @Test
    fun `step ranges cover the video without gaps`() {
        assertEquals(0L until 14_400L, previewBucketRangeMs(0, episode))
        assertEquals(14_400L until 28_800L, previewBucketRangeMs(1, episode))
        assertEquals(1_425_600L until episode, previewBucketRangeMs(99, episode))
        // A 1 min 03 s video ends with a 3 s step
        assertEquals(60_000L until 63_000L, previewBucketRangeMs(12, 63_000))
    }

    private fun cache(maxBytes: Long = 300) = PreviewFrameCache<String>(maxBytes) { 100L }.apply { setSource("a") }

    @Test
    fun `cached frames are keyed by source and step`() {
        val cache = cache()
        cache.put(PreviewFrameKey("a", 1), "a1")
        assertEquals("a1", cache[PreviewFrameKey("a", 1)])
        assertNull(cache[PreviewFrameKey("a", 2)])
        assertNull(cache[PreviewFrameKey("b", 1)])
    }

    @Test
    fun `frames of another source than the current one are refused`() {
        val cache = cache()
        cache.put(PreviewFrameKey("old", 1), "late frame")
        assertFalse(PreviewFrameKey("old", 1) in cache)
        assertEquals(0, cache.size)
    }

    @Test
    fun `another source drops every frame, the same source keeps them`() {
        val cache = cache()
        cache.put(PreviewFrameKey("a", 1), "a1")
        cache.setSource("a")
        assertEquals("a1", cache[PreviewFrameKey("a", 1)])
        cache.setSource("b")
        assertEquals(0, cache.size)
        assertEquals(0L, cache.bytes)
        assertFalse(PreviewFrameKey("a", 1) in cache)
        cache.setSource(null)
        assertNull(cache.sourceId)
    }

    @Test
    fun `cache drops the least recently used frames past its byte budget`() {
        val cache = cache(maxBytes = 300)
        cache.put(PreviewFrameKey("a", 1), "a1")
        cache.put(PreviewFrameKey("a", 2), "a2")
        cache.put(PreviewFrameKey("a", 3), "a3")
        // Reading step 1 makes step 2 the least recently used
        cache[PreviewFrameKey("a", 1)]
        cache.put(PreviewFrameKey("a", 4), "a4")
        assertEquals(3, cache.size)
        assertEquals(300L, cache.bytes)
        assertFalse(PreviewFrameKey("a", 2) in cache)
        assertTrue(PreviewFrameKey("a", 1) in cache)
    }

    @Test
    fun `replacing a frame counts its bytes once`() {
        val cache = cache()
        cache.put(PreviewFrameKey("a", 1), "first")
        cache.put(PreviewFrameKey("a", 1), "second")
        assertEquals(1, cache.size)
        assertEquals(100L, cache.bytes)
    }

    @Test
    fun `only the step under the finger is fetched, once`() {
        val requests = BucketRequests()
        val cached = mutableSetOf<Int>()
        requests.aim(3)
        assertEquals(3, requests.next { it in cached })
        // Still loading: no second request, for it or for another step
        assertNull(requests.next { it in cached })
        requests.aim(4)
        requests.aim(5)
        assertNull(requests.next { it in cached })
        // Done: the step now under the finger comes next, the ones passed over never
        cached += 3
        requests.finished(3)
        assertEquals(5, requests.next { it in cached })
    }

    @Test
    fun `a finger back over the step loading waits for it`() {
        val requests = BucketRequests()
        requests.aim(3)
        assertEquals(3, requests.next { false })
        requests.aim(4)
        requests.aim(3)
        assertNull(requests.next { false })
        assertEquals(3, requests.inFlight)
    }

    @Test
    fun `cached steps are never fetched`() {
        val requests = BucketRequests()
        requests.aim(7)
        assertNull(requests.next { it == 7 })
        assertNull(requests.inFlight)
    }

    @Test
    fun `nothing is fetched once the drag ended, the fetch in flight is kept`() {
        val requests = BucketRequests()
        requests.aim(2)
        assertEquals(2, requests.next { false })
        requests.aim(null)
        assertEquals(2, requests.inFlight)
        requests.finished(2)
        assertNull(requests.next { false })
    }

    @Test
    fun `a stale end of fetch doesn't free the step loading`() {
        val requests = BucketRequests()
        requests.aim(2)
        requests.next { false }
        requests.finished(9)
        assertEquals(2, requests.inFlight)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `frames go on memory pressure, not when the UI hides`() {
        assertFalse(shouldDropPreviewFrames(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN))
        assertFalse(shouldDropPreviewFrames(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE))
        assertTrue(shouldDropPreviewFrames(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW))
        assertTrue(shouldDropPreviewFrames(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL))
        assertTrue(shouldDropPreviewFrames(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND))
        assertTrue(shouldDropPreviewFrames(ComponentCallbacks2.TRIM_MEMORY_COMPLETE))
    }
}
