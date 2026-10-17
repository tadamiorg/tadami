package com.sf.tadami.data.download

import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.utils.DiskUtil
import com.sf.tadami.utils.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadNamesTest {

    private fun episode(id: Long, name: String, url: String = "/ep/$id", number: Float = id.toFloat()) =
        Episode.create().copy(id = id, animeId = 1L, name = name, url = url, episodeNumber = number)

    @Test
    fun sourceDirNameIsStableAndValid() {
        assertEquals("Anime-Sama (FRENCH)", DownloadNames.sourceDirName(42L, "Anime-Sama", Lang.FRENCH))
        assertEquals("A_B (ENGLISH)", DownloadNames.sourceDirName(42L, "A/B", Lang.ENGLISH))
        // An unknown source keeps its id
        assertEquals("42", DownloadNames.sourceDirName(42L, " ", Lang.UNKNOWN))
    }

    @Test
    fun episodeNamesAreMadeValid() {
        val names = DownloadNames.episodeDirNames(
            listOf(episode(1, "Episode 1: The *start*?"), episode(2, "Ep. 2..."), episode(3, ""), episode(4, " ", number = 4.5f)),
        )
        assertEquals("Episode 1_ The _start__", names[1])
        // No trailing dot, FAT and Windows drop it
        assertEquals("Ep. 2", names[2])
        assertEquals("Episode 3", names[3])
        assertEquals("Episode 4.5", names[4])
    }

    @Test
    fun collidingEpisodeNamesGetAUrlSuffix() {
        val episodes = listOf(
            episode(1, "Episode 1", url = "/vostfr/1"),
            episode(2, "episode 1", url = "/vf/1"),
            episode(3, "Episode 2"),
        )
        val names = DownloadNames.episodeDirNames(episodes)

        assertTrue(names.getValue(1).startsWith("Episode 1 ("))
        assertTrue(names.getValue(2).startsWith("episode 1 ("))
        assertNotEquals(names[1]!!.lowercase(), names[2]!!.lowercase())
        // The others keep their plain name
        assertEquals("Episode 2", names[3])
        // Stable: the same names every time
        assertEquals(names, DownloadNames.episodeDirNames(episodes.reversed()))
    }

    @Test
    fun anEpisodeMissingFromTheListIsNamedWithIt() {
        val others = listOf(episode(1, "Episode 1", url = "/a"))
        val name = DownloadNames.episodeDirName(episode(2, "Episode 1", url = "/b"), others)
        assertTrue(name.startsWith("Episode 1 ("))
    }

    @Test
    fun longNamesFitInTheFileNameLimitWithRoomForSuffixes() {
        val long = "あ".repeat(200) // 3 bytes each
        val name = DownloadNames.episodeDirNames(listOf(episode(1, long))).getValue(1)
        val bytes = name.toByteArray(Charsets.UTF_8).size
        assertTrue(bytes <= DiskUtil.MAX_FILE_NAME_BYTES - 64)
        val file = DownloadNames.audioFileName(name, "Japanese", "m4a")
        assertTrue(file.toByteArray(Charsets.UTF_8).size <= DiskUtil.MAX_FILE_NAME_BYTES)
        assertTrue(DownloadNames.animeDirName(long).toByteArray(Charsets.UTF_8).size <= DiskUtil.MAX_FILE_NAME_BYTES)
    }

    @Test
    fun truncationNeverSplitsACharacter() {
        assertEquals("ab", DownloadNames.truncateUtf8("ab😀", 5))
        assertEquals("ab😀", DownloadNames.truncateUtf8("ab😀", 6))
        assertEquals("abc", DownloadNames.truncateUtf8("abc", 10))
    }

    @Test
    fun trackNamesAreUniqueIgnoringCase() {
        assertEquals(
            listOf("English", "english (2)", "Français", "und", "English (3)"),
            DownloadNames.trackNames(listOf("English", "english", "Français", "", "English")),
        )
        assertEquals(listOf("pt_BR"), DownloadNames.trackNames(listOf("pt/BR")))
    }

    @Test
    fun filesAreToldApartFromTheirNames() {
        val base = "Ep. 1"
        val files = DownloadNames.parseFiles(
            base,
            listOf(
                "Ep. 1.ts",
                "Ep. 1.English.vtt",
                "Ep. 1.Français (2).srt",
                "Ep. 1.audio.Japanese.aac",
                // Another episode's, whose name starts the same
                "Ep. 1.5.mp4",
                "notes.txt",
            ),
        )
        assertEquals("Ep. 1.ts", files.video)
        assertEquals(
            listOf(DownloadNames.TrackFile("Ep. 1.audio.Japanese.aac", "Japanese", "aac")),
            files.audios,
        )
        assertEquals(
            listOf(
                DownloadNames.TrackFile("Ep. 1.English.vtt", "English", "vtt"),
                DownloadNames.TrackFile("Ep. 1.Français (2).srt", "Français (2)", "srt"),
            ),
            files.subtitles,
        )
    }

    @Test
    fun aSubtitleInAnAudioNamedLanguageIsASubtitle() {
        val files = DownloadNames.parseFiles("E", listOf("E.mkv", "E.audio.vtt"))
        assertEquals(listOf(DownloadNames.TrackFile("E.audio.vtt", "audio", "vtt")), files.subtitles)
        assertTrue(files.audios.isEmpty())
    }

    @Test
    fun noVideoNoEpisode() {
        assertNull(DownloadNames.parseFiles("E", listOf("E.English.vtt")).video)
    }
}
