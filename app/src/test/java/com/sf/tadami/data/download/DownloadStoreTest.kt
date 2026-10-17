package com.sf.tadami.data.download

import com.sf.tadami.domain.episode.Episode
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

    private fun episode(id: Long, animeId: Long, name: String = "Episode $id") =
        Episode.create().copy(id = id, animeId = animeId, name = name, url = "/anime$animeId/ep/$id", episodeNumber = id.toFloat())

    @Test
    fun theRestoreLeavesOutTheDownloadedEpisodes() {
        val ep1 = episode(1, animeId = 1)
        val ep2 = episode(2, animeId = 1)
        val ep3 = episode(3, animeId = 1)
        val ep4 = episode(4, animeId = 2)
        val dirs = mapOf(
            // ep1 completed before the crash, ep2 was in progress
            1L to listOf(DownloadNames.episodeDirName(ep1), DownloadNames.tmpDirName(DownloadNames.episodeDirName(ep2))),
            2L to emptyList(),
        )
        val reads = mutableListOf<Long>()
        val restored = DownloadStore.withoutDownloaded(listOf(ep1, ep2, ep3, ep4), { it }) { animeId ->
            reads += animeId
            dirs.getValue(animeId)
        }
        assertEquals(listOf(ep2, ep3, ep4), restored)
        // One read per anime
        assertEquals(listOf(1L, 2L), reads)
    }

    @Test
    fun theRestoreFindsAnEpisodeRenamedSinceItsDownload() {
        val downloaded = episode(5, animeId = 1, name = "Episode 5")
        val renamed = downloaded.copy(name = "Episode 5 - The title")
        val restored = DownloadStore.withoutDownloaded(listOf(renamed), { it }) {
            listOf(DownloadNames.episodeDirName(downloaded))
        }
        assertEquals(emptyList<Episode>(), restored)
    }
}
