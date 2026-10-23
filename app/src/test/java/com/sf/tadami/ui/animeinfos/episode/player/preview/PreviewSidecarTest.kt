package com.sf.tadami.ui.animeinfos.episode.player.preview

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/** The seekbar preview sidecar of a downloaded episode: its format, and what makes one unusable. */
class PreviewSidecarTest {

    // 23:50.026: 14.3 s steps, 0..99 for the phone, step 100 a 26 ms leftover
    private val duration = 1_430_026L
    private val videoLength = 812_345_678L

    private fun sidecar(
        entries: List<PreviewSidecar.Entry> = (0..99).map { step ->
            if (step == 29) {
                PreviewSidecar.Entry(step, -1, null)
            } else {
                PreviewSidecar.Entry(step, step * 14_300L + 1_000, ByteArray(100 + step) { (step + it).toByte() })
            }
        },
        durationMs: Long = duration,
        stepMs: Long = previewBucketSizeMs(durationMs),
    ) = PreviewSidecar(videoLength, durationMs, stepMs, 256, 144, entries)

    @Test
    fun `a sidecar reads back as written`() {
        val written = sidecar()
        val read = PreviewSidecar.decode(PreviewSidecar.encode(written), videoLength)!!
        assertEquals(videoLength, read.videoLength)
        assertEquals(duration, read.durationMs)
        assertEquals(14_300L, read.stepMs)
        assertEquals(256, read.frameWidth)
        assertEquals(144, read.frameHeight)
        assertEquals(100, read.entries.size)
        assertEquals(99, read.frameCount)
        read.entries.zip(written.entries).forEach { (actual, expected) ->
            assertEquals(expected.step, actual.step)
            assertEquals(expected.timeMs, actual.timeMs)
            assertArrayEquals(expected.jpeg, actual.jpeg)
        }
        // A step without frame stays one
        val none = read.entries.single { it.step == 29 }
        assertFalse(none.hasFrame)
        assertEquals(-1L, none.timeMs)
    }

    @Test
    fun `an empty sidecar is valid`() {
        val read = PreviewSidecar.decode(PreviewSidecar.encode(sidecar(entries = emptyList())), videoLength)
        assertNotNull(read)
        assertEquals(0, read!!.frameCount)
    }

    @Test
    fun `another video's sidecar is refused`() {
        assertNull(PreviewSidecar.decode(PreviewSidecar.encode(sidecar()), videoLength + 1))
    }

    @Test
    fun `a step size that isn't the duration's is refused`() {
        assertNull(PreviewSidecar.decode(PreviewSidecar.encode(sidecar(stepMs = 14_000)), videoLength))
    }

    @Test
    fun `another format version or no sidecar at all is refused`() {
        val bytes = PreviewSidecar.encode(sidecar())
        // The version, after the magic
        val newer = bytes.copyOf().also { it[7] = (PreviewSidecar.VERSION + 1).toByte() }
        assertNull(PreviewSidecar.decode(newer, videoLength))
        val notOne = bytes.copyOf().also { it[0] = 'X'.code.toByte() }
        assertNull(PreviewSidecar.decode(notOne, videoLength))
        assertNull(PreviewSidecar.decode(ByteArray(0), videoLength))
    }

    @Test
    fun `a truncated sidecar is refused, wherever it was cut`() {
        val bytes = PreviewSidecar.encode(sidecar())
        for (size in listOf(3, 20, 43, 44, 60, bytes.size / 2, bytes.size - 1)) {
            assertNull("cut at $size of ${bytes.size}", PreviewSidecar.decode(bytes.copyOf(size), videoLength))
        }
        // Cut right after an entry: the count tells
        val firstEntryEnd = 44 + 16 + 100
        assertNull(PreviewSidecar.decode(bytes.copyOf(firstEntryEnd), videoLength))
    }

    @Test
    fun `bytes past the last entry are refused`() {
        val bytes = PreviewSidecar.encode(sidecar()) + byteArrayOf(0)
        assertNull(PreviewSidecar.decode(bytes, videoLength))
    }

    @Test
    fun `a corrupted entry is refused`() {
        // One entry whose length is absurd
        val bytes = ByteArrayOutputStream().also { out ->
            DataOutputStream(out).use {
                // The header, without its entry count
                it.write(PreviewSidecar.encode(sidecar(entries = emptyList())).copyOf(40))
                it.writeInt(1)
                it.writeInt(0)
                it.writeLong(0)
                it.writeInt(Int.MAX_VALUE)
            }
        }.toByteArray()
        assertNull(PreviewSidecar.decode(bytes, videoLength))
        // A step past the end of the video
        val past = sidecar(entries = listOf(PreviewSidecar.Entry(101, 0, ByteArray(10))))
        assertNull(PreviewSidecar.decode(PreviewSidecar.encode(past), videoLength))
        // A step twice
        val twice = sidecar(entries = listOf(PreviewSidecar.Entry(3, 0, ByteArray(10)), PreviewSidecar.Entry(3, 0, null)))
        assertNull(PreviewSidecar.decode(PreviewSidecar.encode(twice), videoLength))
    }

    @Test
    fun `the duration must be the player's, within a second`() {
        val read = sidecar()
        assertTrue(read.matchesDuration(duration))
        assertTrue(read.matchesDuration(duration - 1_000))
        assertTrue(read.matchesDuration(duration + 1_000))
        assertFalse(read.matchesDuration(duration + 1_001))
        assertFalse(read.matchesDuration(0))
    }

    @Test
    fun `each step of the player's timeline finds the sidecar's frame of its time`() {
        val read = sidecar()
        // The same duration: the same steps
        (0..99).forEach { step ->
            assertEquals(step, read.entryAt(previewBucketRangeMs(step, duration))?.step)
        }
        // The cast's leftover end step takes the last frame
        assertEquals(99, read.entryAt(previewBucketRangeMs(100, duration))?.step)
        // A receiver whose duration is 300 ms shorter (steps of 14,297 ms): still step for step
        val receiver = duration - 300
        (0..99).forEach { step ->
            assertEquals(step, read.entryAt(previewBucketRangeMs(step, receiver))?.step)
        }
    }
}
