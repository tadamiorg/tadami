package com.sf.tadami.data.download

import com.hippo.unifile.MemoryUniFile
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewSidecar
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketSizeMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The seekbar preview frames kept next to a downloaded episode: written whole, or not at all. */
class EpisodePreviewsTest {

    private val duration = 1_430_026L
    private val dir = MemoryUniFile.root("E")
    private val video = dir.file("E.ts", ByteArray(4))
    private val previews = EpisodePreviews(dir, "E", video)

    private fun sidecar(frames: Int = 3, durationMs: Long = duration) = PreviewSidecar(
        videoLength = video.length(),
        durationMs = durationMs,
        stepMs = previewBucketSizeMs(durationMs),
        frameWidth = 256,
        frameHeight = 144,
        entries = (0 until frames).map { PreviewSidecar.Entry(it, it * 14_300L, byteArrayOf(it.toByte())) },
    )

    private fun write(sidecar: PreviewSidecar) = previews.write(sidecar) { it.openOutputStream() }

    @Test
    fun `written under a temporary name, then renamed`() {
        assertTrue(write(sidecar()))
        assertEquals(setOf("E.ts", "E.preview.tdp"), dir.names)
        assertEquals(3, previews.read()!!.frameCount)
    }

    @Test
    fun `a write that fails leaves nothing behind`() {
        dir.failWrites = true
        assertFalse(write(sidecar()))
        assertEquals(setOf("E.ts"), dir.names)
        assertNull(previews.read())
    }

    @Test
    fun `a storage folder renaming to another name leaves nothing behind`() {
        // A name taken already is renamed "<name> (1)" by some storage folders: never kept so
        dir.renameSuffix = " (1)"
        assertFalse(write(sidecar()))
        assertEquals(setOf("E.ts"), dir.names)
        assertNull(previews.read())
    }

    @Test
    fun `nothing for a video gone or changed`() {
        assertFalse(previews.write(sidecar().let { PreviewSidecar(5, it.durationMs, it.stepMs, 256, 144, it.entries) }) { it.openOutputStream() })
        video.delete()
        assertFalse(write(sidecar()))
        assertEquals(emptySet<String>(), dir.names)
    }

    @Test
    fun `replaces older frames`() {
        assertTrue(write(sidecar(frames = 2)))
        assertNotNull(previews.read())
        assertTrue(write(sidecar(frames = 3, durationMs = duration + 5_000)))
        assertEquals(setOf("E.ts", "E.preview.tdp"), dir.names)
        assertEquals(3, previews.read()!!.frameCount)
    }
}
