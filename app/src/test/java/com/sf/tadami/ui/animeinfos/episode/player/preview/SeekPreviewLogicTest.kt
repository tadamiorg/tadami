package com.sf.tadami.ui.animeinfos.episode.player.preview

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekPreviewLogicTest {

    // Pushes a value and applies it if its turn has come, as a drag does
    private fun LatestThrottle.offer(value: Long, nowMs: Long): Long? {
        push(value)
        return poll(nowMs)
    }

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

    @Test
    fun `main player spares the network with enough buffered ahead`() {
        assertTrue(mainPlayerCanSpareNetwork(isBuffering = false, bufferedAheadMs = 10_000, bufferedPositionMs = 70_000, durationMs = 1_400_000))
        assertFalse(mainPlayerCanSpareNetwork(isBuffering = false, bufferedAheadMs = 9_999, bufferedPositionMs = 70_000, durationMs = 1_400_000))
    }

    @Test
    fun `main player never spares the network while rebuffering`() {
        assertFalse(mainPlayerCanSpareNetwork(isBuffering = true, bufferedAheadMs = 60_000, bufferedPositionMs = 1_400_000, durationMs = 1_400_000))
    }

    @Test
    fun `main player loaded to the end spares the network`() {
        assertTrue(mainPlayerCanSpareNetwork(isBuffering = false, bufferedAheadMs = 2_000, bufferedPositionMs = 1_400_000, durationMs = 1_400_000))
        // Unknown duration: only the buffer ahead counts
        assertFalse(mainPlayerCanSpareNetwork(isBuffering = false, bufferedAheadMs = 2_000, bufferedPositionMs = 0, durationMs = C.TIME_UNSET))
    }

    @Test
    fun `pushed value waits for its turn`() {
        val throttle = LatestThrottle(intervalMs = 100)
        assertFalse(throttle.hasPending)
        throttle.push(1_000)
        throttle.push(2_000)
        assertTrue(throttle.hasPending)
        assertEquals(2_000L, throttle.poll(nowMs = 0))
        assertFalse(throttle.hasPending)
    }

    @Test
    fun `throttle applies the first value at once`() {
        val throttle = LatestThrottle(intervalMs = 100)
        assertEquals(1_000L, throttle.offer(1_000, nowMs = 0))
    }

    @Test
    fun `throttle keeps only the newest value offered within the interval`() {
        val throttle = LatestThrottle(intervalMs = 100)
        throttle.offer(1_000, nowMs = 0)
        assertNull(throttle.offer(2_000, nowMs = 10))
        assertNull(throttle.offer(3_000, nowMs = 40))
        assertEquals(60L, throttle.delayMs(nowMs = 40))
        assertNull(throttle.poll(nowMs = 99))
        // The stale 2 000 is never applied
        assertEquals(3_000L, throttle.poll(nowMs = 100))
        assertNull(throttle.poll(nowMs = 300))
    }

    @Test
    fun `throttle applies a value at once after a quiet interval`() {
        val throttle = LatestThrottle(intervalMs = 100)
        throttle.offer(1_000, nowMs = 0)
        assertEquals(2_000L, throttle.offer(2_000, nowMs = 150))
        assertEquals(0L, throttle.delayMs(nowMs = 150))
    }

    @Test
    fun `throttle interval counts from the last applied value`() {
        val throttle = LatestThrottle(intervalMs = 100)
        throttle.offer(1_000, nowMs = 0)
        throttle.offer(2_000, nowMs = 50)
        assertEquals(2_000L, throttle.poll(nowMs = 120))
        assertNull(throttle.offer(3_000, nowMs = 180))
        assertEquals(40L, throttle.delayMs(nowMs = 180))
    }

    @Test
    fun `cleared throttle drops the waiting value and lets the next one through`() {
        val throttle = LatestThrottle(intervalMs = 100)
        throttle.offer(1_000, nowMs = 0)
        throttle.offer(2_000, nowMs = 10)
        throttle.clear()
        assertNull(throttle.poll(nowMs = 200))
        assertEquals(3_000L, throttle.offer(3_000, nowMs = 20))
    }
}
