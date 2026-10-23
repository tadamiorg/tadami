package com.sf.tadami.data.download

import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.utils.DiskUtil
import com.sf.tadami.utils.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private fun base(episode: Episode) = DownloadNames.episodeDirName(episode).substringBeforeLast(" (")

    @Test
    fun episodeNamesAreMadeValid() {
        assertEquals("Episode 1_ The _start__", base(episode(1, "Episode 1: The *start*?")))
        // No trailing dot, FAT and Windows drop it
        assertEquals("Ep. 2", base(episode(2, "Ep. 2...")))
        assertEquals("Episode 3", base(episode(3, "")))
        assertEquals("Episode 4.5", base(episode(4, " ", number = 4.5f)))
    }

    @Test
    fun episodeDirsAreKeyedOnTheUrl() {
        val vostfr = DownloadNames.episodeDirName(episode(1, "Episode 1", url = "/vostfr/1"))
        val vf = DownloadNames.episodeDirName(episode(2, "episode 1", url = "/vf/1"))

        assertEquals("Episode 1 (${DownloadNames.episodeKey("/vostfr/1")})", vostfr)
        assertEquals(8, DownloadNames.episodeKey("/vostfr/1").length)
        assertNotEquals(vostfr.lowercase(), vf.lowercase())
        // URLs that differ only at the end get different keys too
        assertNotEquals(DownloadNames.episodeKey("/ep?id=1"), DownloadNames.episodeKey("/ep?id=2"))
        // Stable: the same name every time
        assertEquals(vostfr, DownloadNames.episodeDirName(episode(1, "Episode 1", url = "/vostfr/1")))
        assertEquals(DownloadNames.episodeKey("/vostfr/1"), DownloadNames.episodeKeyOf(vostfr))
        // An anime key isn't an episode key
        assertNull(DownloadNames.episodeKeyOf(DownloadNames.animeDirName("Frieren", "/a")))
    }

    @Test
    fun episodeDirIsFoundByItsKeyAfterARename() {
        val renamed = episode(1, "Episode 1 - The start", url = "/ep/1")
        val old = DownloadNames.episodeDirName(episode(1, "Episode 1", url = "/ep/1"))
        val other = DownloadNames.episodeDirName(episode(2, "Episode 2", url = "/ep/2"))
        val dirs = DownloadNames.EpisodeDirs(listOf(old, other))

        assertEquals(old, dirs.find(renamed))
        assertEquals(other, dirs.find(episode(2, "Episode 2", url = "/ep/2")))
        assertNull(dirs.find(episode(3, "Episode 1", url = "/ep/3")))
        // Its own name first
        val current = DownloadNames.episodeDirName(renamed)
        assertEquals(current, DownloadNames.EpisodeDirs(listOf(old, current)).find(renamed))
    }

    @Test
    fun episodeDirsOfTheEarlierLayoutAreFound() {
        val plain = episode(1, "Episode 1", url = "/ep/1")
        val collided = episode(2, "Episode 2", url = "/ep/2")
        val collidedDir = "Episode 2 (${DownloadNames.animeKey("/ep/2")})"
        val dirs = DownloadNames.EpisodeDirs(listOf("Episode 1", collidedDir, "Episode 3_tmp"))

        assertEquals("Episode 1", dirs.find(plain))
        assertEquals(collidedDir, dirs.find(collided))
        // A partial download isn't one
        assertNull(dirs.find(episode(3, "Episode 3", url = "/ep/3")))
    }

    @Test
    fun longNamesFitInTheFileNameLimitWithRoomForSuffixes() {
        val long = "あ".repeat(200) // 3 bytes each
        val name = DownloadNames.episodeDirName(episode(1, long))
        val bytes = DownloadNames.tmpDirName(name).toByteArray(Charsets.UTF_8).size
        assertTrue(bytes <= DiskUtil.MAX_FILE_NAME_BYTES)
        // The key survives the cut
        assertEquals(DownloadNames.episodeKey("/ep/1"), DownloadNames.episodeKeyOf(name))
        val file = DownloadNames.audioFileName(name, "Japanese", "m4a")
        assertTrue(file.toByteArray(Charsets.UTF_8).size <= DiskUtil.MAX_FILE_NAME_BYTES)
        val animeDir = DownloadNames.animeDirName(long, "/anime/1")
        assertTrue(animeDir.toByteArray(Charsets.UTF_8).size <= DiskUtil.MAX_FILE_NAME_BYTES)
        // The key survives the cut
        assertEquals(DownloadNames.animeKey("/anime/1"), DownloadNames.animeKeyOf(animeDir))
    }

    @Test
    fun animeWithTheSameTitleGetDistinctDirs() {
        val vostfr = DownloadNames.animeDirName("Frieren", "/catalogue/frieren/vostfr")
        val vf = DownloadNames.animeDirName("Frieren", "/catalogue/frieren/vf")

        assertTrue(vostfr.startsWith("Frieren ("))
        assertNotEquals(vostfr.lowercase(), vf.lowercase())
        // Stable: the same name every time
        assertEquals(vostfr, DownloadNames.animeDirName("Frieren", "/catalogue/frieren/vostfr"))
        assertEquals("A_B (${DownloadNames.animeKey("/a")})", DownloadNames.animeDirName("A/B", "/a"))
    }

    @Test
    fun animeDirIsFoundByItsKeyAfterATitleChange() {
        val url = "/anime/42"
        val old = DownloadNames.animeDirName("Old title", url)
        val other = DownloadNames.animeDirName("New title", "/anime/43")
        val expected = DownloadNames.animeDirName("New title", url)

        assertEquals(old, DownloadNames.findAnimeDirName(listOf(other, old), expected))
        // Its own name first, an older one only when it is missing
        assertEquals(expected, DownloadNames.findAnimeDirName(listOf(old, expected), expected))
        assertNull(DownloadNames.findAnimeDirName(listOf(other, "New title"), expected))
        assertNull(DownloadNames.animeKeyOf("New title"))
        assertNull(DownloadNames.animeKeyOf("Season (2)"))
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

    @Test
    fun theSidecarIsNeitherAVideoNorATrack() {
        // "E.json" sorts before "E.ts": it must not be taken for the video
        val files = DownloadNames.parseFiles("E", listOf("E.ts", "E.json", "E.English.vtt"))
        assertEquals("E.ts", files.video)
        assertEquals("E.json", files.metadata)
        assertEquals(listOf("E.English.vtt"), files.subtitles.map { it.name })
        assertTrue(files.audios.isEmpty())
        // A sidecar alone is no episode
        assertNull(DownloadNames.parseFiles("E", listOf("E.json")).video)
        // Downloads made before it have none
        assertNull(DownloadNames.parseFiles("E", listOf("E.ts")).metadata)
    }

    @Test
    fun theSeekbarPreviewFramesAreNeitherAVideoNorATrack() {
        // Complete, and half written: "E.preview..." sorts before "E.ts", neither may be taken for the video
        val names = listOf("E.ts", "E.preview.tdp", "E.preview-tmp.tdp", "E.json", "E.English.vtt")
        val files = DownloadNames.parseFiles("E", names)
        assertEquals("E.ts", files.video)
        assertEquals("E.preview.tdp", files.previews)
        assertEquals("E.json", files.metadata)
        assertEquals(listOf("E.English.vtt"), files.subtitles.map { it.name })
        assertTrue(files.audios.isEmpty())
        assertEquals("E.preview.tdp", DownloadNames.previewsFileName("E"))
        assertEquals("E.preview-tmp.tdp", DownloadNames.previewsTmpFileName("E"))
        // Frames alone are no episode
        assertNull(DownloadNames.parseFiles("E", listOf("E.preview.tdp", "E.preview-tmp.tdp")).video)
        // None until they are made
        assertNull(DownloadNames.parseFiles("E", listOf("E.mp4")).previews)
        // A version without them sees neither a video in them (it takes "<episode>.<ext>" for one), nor a track
        val audioPrefix = DownloadNames.audioFileBase("E", "").removePrefix("E.")
        listOf(DownloadNames.previewsFileName("E"), DownloadNames.previewsTmpFileName("E")).forEach { name ->
            val rest = name.removePrefix("E.")
            assertTrue(name, '.' in rest)
            assertFalse(name, rest.substringAfterLast('.').lowercase() in DownloadNames.SUBTITLE_EXTENSIONS)
            assertFalse(name, rest.startsWith(audioPrefix))
        }
    }
}
