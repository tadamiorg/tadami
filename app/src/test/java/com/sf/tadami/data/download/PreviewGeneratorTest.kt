package com.sf.tadami.data.download

import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewSidecar
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketRangeMs
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The seekbar preview frames made after a download: which episodes, in what order, and how. */
class PreviewGeneratorTest {

    // 23:50.026: 14.3 s steps, 0..99 for the phone, the 26 ms leftover merged into step 99
    private val duration = 1_430_026L

    private inner class FakeDecoder(
        private val durationMs: Long? = duration,
        private val noFrame: Set<Long> = emptySet(),
        private val failAt: Long? = null,
        private val onFrame: (Long) -> Unit = {},
    ) : PreviewFrameDecoder {
        val asked = mutableListOf<Pair<Long, Long>>()
        var released = false

        override suspend fun durationMs() = durationMs

        override suspend fun frame(positionMs: Long, toleranceMs: Long): PreviewFrameDecoder.DecodedFrame? {
            assertFalse("used after its release", released)
            asked += positionMs to toleranceMs
            onFrame(positionMs)
            if (positionMs == failAt) throw PreviewFrameDecoder.FailedException()
            if (positionMs in noFrame) return null
            return PreviewFrameDecoder.DecodedFrame(byteArrayOf(positionMs.toByte()), 256, 144, positionMs - 100)
        }

        override fun release() {
            released = true
        }
    }

    @Test
    fun `an episode's frames are made only with both settings on, once, and not once deleted`() {
        assertEquals(PreviewGenerationDecision.GENERATE, previewGenerationDecision(settingsOn = true, videoExists = true, hasValidSidecar = false))
        assertEquals(PreviewGenerationDecision.SETTINGS_OFF, previewGenerationDecision(settingsOn = false, videoExists = true, hasValidSidecar = false))
        assertEquals(PreviewGenerationDecision.DONE, previewGenerationDecision(settingsOn = true, videoExists = true, hasValidSidecar = true))
        assertEquals(PreviewGenerationDecision.DELETED, previewGenerationDecision(settingsOn = true, videoExists = false, hasValidSidecar = false))
        assertEquals(PreviewGenerationDecision.DELETED, previewGenerationDecision(settingsOn = false, videoExists = false, hasValidSidecar = true))
    }

    @Test
    fun `episodes wait in download order, each once`() {
        val a = PendingPreviews("S", "A", "Ep 1")
        val b = PendingPreviews("S", "A", "Ep 2")
        val queue = PreviewQueue(listOf(a, b, a))
        assertEquals(listOf(a, b), queue.entries)
        assertFalse("waiting already", queue.add(b))
        val c = PendingPreviews("S", "B", "Ep 1")
        assertTrue(queue.add(c))
        assertEquals(a, queue.first)
        queue.remove(a)
        assertEquals(listOf(b, c), queue.entries)
    }

    @Test
    fun `one frame per phone step, from the middle of each, any frame of the step will do`() = runBlocking {
        val decoder = FakeDecoder(noFrame = setOf(previewBucketRangeMs(29, duration).let { (it.first + it.last + 1) / 2 }))
        val sidecar = makePreviewSidecar(videoLength = 42, newDecoder = { decoder }, paused = MutableStateFlow(false))!!
        assertEquals(42L, sidecar.videoLength)
        assertEquals(duration, sidecar.durationMs)
        assertEquals(14_300L, sidecar.stepMs)
        assertEquals(256 to 144, sidecar.frameWidth to sidecar.frameHeight)
        assertEquals((0..99).toList(), sidecar.entries.map { it.step })
        assertEquals(listOf(7_150L to 7_150L, 21_450L to 7_150L), decoder.asked.take(2))
        // The step without a frame in time is kept as such
        assertEquals(99, sidecar.frameCount)
        assertFalse(sidecar.entries[29].hasFrame)
        // Each frame's own time
        assertEquals(7_050L, sidecar.entries[0].timeMs)
        assertTrue(decoder.released)
        // Readable as written
        assertEquals(99, PreviewSidecar.decode(PreviewSidecar.encode(sidecar), 42)!!.frameCount)
    }

    @Test
    fun `nothing for a video that can't be decoded`() = runBlocking {
        val paused = MutableStateFlow(false)
        assertNull(makePreviewSidecar(42, { FakeDecoder(durationMs = null) }, paused))
        val failing = FakeDecoder(failAt = 21_450)
        assertNull(makePreviewSidecar(42, { failing }, paused))
        assertTrue("released", failing.released)
        assertNull("not a single frame", makePreviewSidecar(42, { FakeDecoder(noFrame = (0..100L).map { it * 14_300 + 7_150 }.toSet()) }, paused))
    }

    @Test
    fun `paused while the player screen is active, the decoder released, then it goes on where it was`() = runBlocking {
        val paused = MutableStateFlow(false)
        val decoders = mutableListOf<FakeDecoder>()
        var pauses = 0
        val result = async {
            makePreviewSidecar(
                videoLength = 42,
                newDecoder = {
                    FakeDecoder(onFrame = { position ->
                        // The player screen opens while step 10 decodes
                        if (position == previewBucketRangeMs(10, duration).let { (it.first + it.last + 1) / 2 }) paused.value = true
                    }).also { decoders += it }
                },
                paused = paused,
                onPause = { pauses++ },
            )
        }
        withTimeout(5_000) { while (pauses == 0) delay(10) }
        // Nothing decodes while it waits, its decoder is free
        delay(100)
        assertEquals(1, decoders.size)
        assertTrue(decoders[0].released)
        assertEquals(11, decoders[0].asked.size)
        paused.value = false
        val sidecar = withTimeout(5_000) { result.await() }!!
        assertEquals(2, decoders.size)
        assertEquals("every step once", (0..99).toList(), sidecar.entries.map { it.step })
        assertEquals(100, decoders.sumOf { it.asked.size })
        assertTrue(decoders.all { it.released })
    }

    @Test
    fun `waits from the start while the player screen is active`() = runBlocking {
        val paused = MutableStateFlow(true)
        var created = 0
        val result = async { makePreviewSidecar(42, { created++; FakeDecoder() }, paused) }
        delay(100)
        assertEquals(0, created)
        paused.value = false
        assertEquals(100, withTimeout(5_000) { result.await() }!!.frameCount)
        assertEquals(1, created)
    }
}
