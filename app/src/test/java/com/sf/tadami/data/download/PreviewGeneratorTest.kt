package com.sf.tadami.data.download

import androidx.media3.common.PlaybackException
import com.hippo.unifile.MemoryUniFile
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewSidecar
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketRangeMs
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketSizeMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** The seekbar preview frames made after a download: which episodes, in what order, and how. */
class PreviewGeneratorTest {

    // 23:50.026: 14.3 s steps, 0..99 for the phone, the 26 ms leftover merged into step 99
    private val duration = 1_430_026L

    private inner class FakeDecoder(
        private val durationMs: Long? = duration,
        private val noFrame: Set<Long> = emptySet(),
        // The errors at a position, in turn, before its frame
        failures: Map<Long, List<Int>> = emptyMap(),
        private val onFrame: (Long) -> Unit = {},
    ) : PreviewFrameDecoder {
        val asked = mutableListOf<Pair<Long, Long>>()
        var released = false
        private val failures = failures.mapValues { ArrayDeque(it.value) }

        override suspend fun durationMs() = durationMs

        override suspend fun frame(positionMs: Long, toleranceMs: Long): PreviewFrameDecoder.DecodedFrame? {
            assertFalse("used after its release", released)
            asked += positionMs to toleranceMs
            onFrame(positionMs)
            failures[positionMs]?.removeFirstOrNull()?.let { throw PreviewFrameDecoder.FailedException(it) }
            if (positionMs in noFrame) return null
            return PreviewFrameDecoder.DecodedFrame(byteArrayOf(positionMs.toByte()), 256, 144, positionMs - 100)
        }

        override fun release() {
            released = true
        }
    }

    private fun middleOf(step: Int) = previewBucketRangeMs(step, duration).let { (it.first + it.last + 1) / 2 }

    private fun sidecarOf(made: MadePreviews) = (made as MadePreviews.Done).sidecar

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
        assertEquals(a, queue.start())
        // Waiting still while its frames are made
        assertEquals(listOf(a, b, c), queue.entries)
        queue.finish()
        assertEquals(listOf(b, c), queue.entries)
        // Stopped: it waits still
        assertEquals(b, queue.start())
        queue.finish(kept = true)
        assertEquals(listOf(b, c), queue.entries)
    }

    @Test
    fun `an episode downloaded again while its frames are made comes again`() {
        val a = PendingPreviews("S", "A", "Ep 1")
        val queue = PreviewQueue(listOf(a))
        assertEquals(a, queue.start())
        assertFalse("waiting already", queue.add(a))
        assertTrue(queue.startedDownloadedAgain)
        queue.finish()
        assertEquals(listOf(a), queue.entries)
        // Its turn again, made from the new file
        assertEquals(a, queue.start())
        assertFalse(queue.startedDownloadedAgain)
        queue.finish()
        assertTrue(queue.entries.isEmpty())
        assertNull(queue.start())
    }

    @Test
    fun `one frame per phone step, from the middle of each, any frame of the step will do`() = runBlocking {
        val decoder = FakeDecoder(noFrame = setOf(middleOf(29)))
        val sidecar = sidecarOf(makePreviewSidecar(videoLength = 42, newDecoder = { decoder }))
        assertEquals(42L, sidecar.videoLength)
        assertEquals(duration, sidecar.durationMs)
        assertEquals(14_300L, sidecar.stepMs)
        assertEquals(256 to 144, sidecar.frameWidth to sidecar.frameHeight)
        assertEquals(listOf(7_150L to 7_150L, 21_450L to 7_150L), decoder.asked.take(2))
        assertEquals(100, decoder.asked.size)
        // The step without a frame in time is left out, not kept as one without: fetched when it's needed
        assertEquals((0..99).filter { it != 29 }, sidecar.entries.map { it.step })
        assertEquals(99, sidecar.frameCount)
        // Each frame's own time
        assertEquals(7_050L, sidecar.entries[0].timeMs)
        assertTrue(decoder.released)
        // Readable as written
        assertEquals(99, PreviewSidecar.decode(PreviewSidecar.encode(sidecar), 42)!!.frameCount)
    }

    @Test
    fun `nothing for a video that can't be decoded`() = runBlocking {
        assertSame(MadePreviews.Failed, makePreviewSidecar(42, { FakeDecoder(durationMs = null) }))
        // The third error gives up on the video
        val failing = FakeDecoder(failures = listOf(1, 5, 9).associate { middleOf(it) to listOf(PlaybackException.ERROR_CODE_IO_UNSPECIFIED) })
        assertSame(MadePreviews.Failed, makePreviewSidecar(42, { failing }))
        assertTrue("released", failing.released)
        assertEquals("stopped at the third", middleOf(9), failing.asked.last().first)
        assertSame("not a single frame", MadePreviews.Failed, makePreviewSidecar(42, { FakeDecoder(noFrame = (0..100L).map { it * 14_300 + 7_150 }.toSet()) }))
    }

    @Test
    fun `an error costs its step only, a decoder taken back or that couldn't start tries it again`() = runBlocking {
        // A part of the file that can't be read: its step is left out
        val unreadable = sidecarOf(makePreviewSidecar(42, { FakeDecoder(failures = mapOf(middleOf(5) to listOf(PlaybackException.ERROR_CODE_IO_UNSPECIFIED))) }))
        assertEquals((0..99).filter { it != 5 }, unreadable.entries.map { it.step })
        // The decoder taken back (by the player, another app), then not starting: the step comes the third time
        val decoder = FakeDecoder(
            failures = mapOf(
                middleOf(5) to listOf(
                    PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                ),
            ),
        )
        val retried = sidecarOf(makePreviewSidecar(42, { decoder }))
        assertEquals((0..99).toList(), retried.entries.map { it.step })
        assertEquals(3, decoder.asked.count { it.first == middleOf(5) })
        // Taken back over and over: given up, as the player's preview does
        val reclaimed = List(3) { PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED }
        assertSame(MadePreviews.Failed, makePreviewSidecar(42, { FakeDecoder(failures = mapOf(middleOf(5) to reclaimed)) }))
    }

    @Test
    fun `stopped while the player screen or a cast is active, the decoder released`() = runBlocking {
        var active = false
        // The player screen opens while step 10 decodes
        val decoder = FakeDecoder(onFrame = { if (it == middleOf(10)) active = true })
        assertSame(MadePreviews.Stopped, makePreviewSidecar(42, { decoder }, stopped = { active }))
        assertEquals("nothing after step 10", 11, decoder.asked.size)
        assertTrue(decoder.released)
        // Active from the start: not even a decoder
        var created = 0
        assertSame(MadePreviews.Stopped, makePreviewSidecar(42, { created++; FakeDecoder() }, stopped = { true }))
        assertEquals(0, created)
    }

    // --- The generator: its queue, saved, and each episode's turn ---------------------------------

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val storeDir: File = Files.createTempDirectory("previews").toFile()
    private val store = File(storeDir, "previews.json")
    private val downloads = MemoryUniFile.root("Downloads")
    private val episodes = mutableMapOf<String, EpisodePreviews>()
    private val paused = MutableStateFlow(false)
    private var settingsOn = true
    private var schedules = 0
    private val decoders = mutableListOf<FakeDecoder>()
    private var onFrame: (Long) -> Unit = {}

    @After
    fun tearDown() {
        scope.cancel()
        storeDir.deleteRecursively()
    }

    /** A downloaded episode [name], its video [bytes] long. */
    private fun episode(name: String, bytes: Int = 4): PendingPreviews {
        val dir = downloads.folder(name)
        episodes[name] = EpisodePreviews(dir, name, dir.file("$name.ts", ByteArray(bytes)))
        return PendingPreviews("S", "A", name)
    }

    private fun generator() = PreviewGenerator(
        store = store,
        settingsOn = { settingsOn },
        previewsOf = { episodes[it.episodeDir]?.takeIf { previews -> previews.dir.exists() } },
        newDecoder = { FakeDecoder(onFrame = onFrame).also { decoders += it } },
        write = { previews, sidecar -> previews.write(sidecar) { it.openOutputStream() } },
        schedule = { schedules++ },
        paused = paused,
        scope = scope,
    )

    private fun PreviewGenerator.downloaded(entry: PendingPreviews) = onDownloaded(entry.sourceDir, entry.animeDir, entry.episodeDir)

    @Test
    fun `the episodes waiting are saved, and the next process makes them`() {
        val a = episode("Ep 1")
        val b = episode("Ep 2")
        val first = generator()
        first.downloaded(a)
        first.downloaded(b)
        first.downloaded(a)
        assertEquals(listOf(a, b), first.waiting)
        assertTrue(store.exists())
        assertEquals("one run for both", 1, schedules)
        // A process death: the next one finds them, and has them made
        val next = generator()
        assertEquals(listOf(a, b), next.waiting)
        assertEquals(2, schedules)
        runBlocking { next.makeAll() }
        assertTrue(next.waiting.isEmpty())
        assertFalse("nothing left to save", store.exists())
        assertEquals(100, episodes.getValue("Ep 1").read()!!.frameCount)
        assertEquals(100, episodes.getValue("Ep 2").read()!!.frameCount)
    }

    @Test
    fun `nothing made with the settings off, for an episode deleted, or whose frames are there`() {
        val off = episode("Ep 1")
        val deleted = episode("Ep 2")
        val made = episode("Ep 3")
        val generator = generator()
        generator.downloaded(deleted)
        generator.downloaded(made)
        episodes.getValue("Ep 2").dir.delete()
        val existing = PreviewSidecar(4, duration, previewBucketSizeMs(duration), 256, 144, listOf(PreviewSidecar.Entry(0, 0, byteArrayOf(1))))
        assertTrue(episodes.getValue("Ep 3").write(existing) { it.openOutputStream() })
        runBlocking { generator.makeAll() }
        assertEquals("never decoded", 0, decoders.size)
        assertEquals(1, episodes.getValue("Ep 3").read()!!.frameCount)
        settingsOn = false
        generator.downloaded(off)
        runBlocking { generator.makeAll() }
        assertEquals(0, decoders.size)
        assertNull(episodes.getValue("Ep 1").read())
        assertTrue(generator.waiting.isEmpty())
    }

    @Test
    fun `stopped by the player screen, the episode waits, then is made unless its frames came meanwhile`() {
        val a = episode("Ep 1")
        val b = episode("Ep 2")
        val generator = generator()
        generator.downloaded(a)
        generator.downloaded(b)
        assertEquals(1, schedules)
        // The player screen opens as step 10 of the first episode decodes
        onFrame = { if (it == middleOf(10)) paused.value = true }
        runBlocking { generator.makeAll() }
        assertEquals(listOf(a, b), generator.waiting)
        assertTrue(store.exists())
        assertNull(episodes.getValue("Ep 1").read())
        assertTrue(decoders.single().released)
        // Nothing runs while it's active, nor for an episode downloaded meanwhile
        generator.downloaded(episode("Ep 3"))
        assertEquals(1, schedules)
        // Its playback made the first episode's frames
        onFrame = {}
        val played = PreviewSidecar(4, duration, previewBucketSizeMs(duration), 256, 144, listOf(PreviewSidecar.Entry(0, 0, byteArrayOf(1))))
        assertTrue(episodes.getValue("Ep 1").write(played) { it.openOutputStream() })
        // Gone: a run again, the first episode not decoded again
        paused.value = false
        assertEquals(2, schedules)
        runBlocking { generator.makeAll() }
        assertEquals(1, episodes.getValue("Ep 1").read()!!.frameCount)
        assertEquals(100, episodes.getValue("Ep 2").read()!!.frameCount)
        assertEquals("the first stopped, then the second and the third", 3, decoders.size)
        assertTrue(generator.waiting.isEmpty())
    }

    @Test
    fun `an episode downloaded again while its frames are made has them made from its new file`() {
        val a = episode("Ep 1")
        val generator = generator()
        generator.downloaded(a)
        // Downloaded again as step 10 decodes: a longer file in the same folder
        var again = false
        onFrame = {
            if (it == middleOf(10) && !again) {
                again = true
                val dir = episodes.getValue("Ep 1").dir as MemoryUniFile
                episodes["Ep 1"] = EpisodePreviews(dir, "Ep 1", dir.file("Ep 1.ts", ByteArray(8)))
                generator.downloaded(a)
            }
        }
        runBlocking { generator.makeAll() }
        assertEquals("the old file's decode stopped, the new one's whole", listOf(11, 100), decoders.map { it.asked.size })
        assertEquals(8L, episodes.getValue("Ep 1").read()!!.videoLength)
        assertTrue(generator.waiting.isEmpty())
    }

    @Test
    fun `an episode downloaded once a run has ended has a run of its own`() {
        val generator = generator()
        assertEquals("nothing waiting", 0, schedules)
        generator.downloaded(episode("Ep 1"))
        generator.downloaded(episode("Ep 2"))
        assertEquals(1, schedules)
        runBlocking { generator.makeAll() }
        generator.downloaded(episode("Ep 3"))
        assertEquals(2, schedules)
        runBlocking { generator.makeAll() }
        assertNotNull(episodes.getValue("Ep 3").read())
    }
}
