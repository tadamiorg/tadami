package com.sf.tadami.data.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloadStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun theQueueSurvivesInOrder() {
        val file = File(folder.root, "downloads/queue.json")
        val entries = listOf(
            DownloadStore.Entry(episodeId = 3, animeId = 1, sourceId = 10, dirName = "Episode 3"),
            DownloadStore.Entry(episodeId = 1, animeId = 1, sourceId = 10, dirName = "Episode 1 (a1b2c3)"),
            DownloadStore.Entry(episodeId = 7, animeId = 2, sourceId = 11, dirName = "Ep. 7"),
        )
        DownloadStore(file).save(entries)

        // As after a process death: a new store on the same file
        assertEquals(entries, DownloadStore(file).restore())
    }

    @Test
    fun anEmptyQueueLeavesNoFile() {
        val file = File(folder.root, "queue.json")
        val store = DownloadStore(file)
        store.save(listOf(DownloadStore.Entry(1, 1, 1, "E")))
        store.save(emptyList())

        assertFalse(file.exists())
        assertEquals(emptyList<DownloadStore.Entry>(), store.restore())
    }

    @Test
    fun aDamagedFileIsAnEmptyQueue() {
        val file = folder.newFile("queue.json").apply { writeText("{not json") }
        assertEquals(emptyList<DownloadStore.Entry>(), DownloadStore(file).restore())
    }

    @Test
    fun resumeDataSurvivesPerEpisode() {
        val dir = File(folder.root, "resume")
        val data = ResumeData(
            stream = "Kwik|1080p|Kwik 1080p",
            failed = listOf("Sendvid||Sendvid"),
            files = mapOf(
                "video" to FileProgress(name = "Ep. 1.ts", hls = true, bytes = 123_456, segments = 42, total = 600),
                "audio0" to FileProgress(name = "Ep. 1.audio.Japanese.mp4", hls = false, total = 9_999),
            ),
        )
        ResumeStore(dir).save(5L, data)

        val store = ResumeStore(dir)
        assertEquals(data, store.load(5L))
        assertNull(store.load(6L))

        store.delete(5L)
        assertNull(store.load(5L))
        assertTrue(dir.exists())
    }
}
