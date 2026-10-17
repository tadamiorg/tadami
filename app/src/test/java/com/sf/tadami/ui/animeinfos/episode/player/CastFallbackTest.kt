package com.sf.tadami.ui.animeinfos.episode.player

import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.model.Track
import com.sf.tadami.ui.animeinfos.episode.player.CastFallback.AfterCast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CastFallbackTest {

    private val local = StreamSource(url = "content://com.android.externalstorage.documents/tree/primary/Ep 1.ts", fullName = "Downloaded")
    private val online = StreamSource(url = "https://host/ep1/master.m3u8", fullName = "Server - 1080p")

    @Test
    fun theCastEpisodeGoesBackToItsFiles() {
        assertEquals(AfterCast.Restore(local), CastFallback.afterCast(1L, online, beforeCast = 1L to local))
    }

    @Test
    fun anotherEpisodeSwitchedToDuringTheCastLooksForItsFiles() {
        // The TV moved on to episode 2: episode 1's files don't fit
        assertEquals(AfterCast.Lookup, CastFallback.afterCast(2L, online, beforeCast = 1L to local))
        // The cast was found running when the player opened: nothing was replaced here
        assertEquals(AfterCast.Lookup, CastFallback.afterCast(2L, online, beforeCast = null))
    }

    @Test
    fun theFilesPlayingAlreadyOrNoEpisodeKeepThePlayer() {
        assertEquals(AfterCast.Keep, CastFallback.afterCast(1L, local, beforeCast = 1L to local))
        assertEquals(AfterCast.Keep, CastFallback.afterCast(null, online, beforeCast = 1L to local))
    }

    @Test
    fun theFilesPlayAgainOnlyWhenAllAreStillThere() {
        val dir = "content://com.android.externalstorage.documents/tree/primary/Ep 1"
        val withTracks = local.copy(
            audioTracks = listOf(Track.AudioTrack(url = "$dir.audio.Japanese.ts", lang = "Japanese")),
            subtitleTracks = listOf(Track.SubtitleTrack(url = "$dir.sub.French.vtt", lang = "French")),
        )
        val all = setOf(local.url, "$dir.audio.Japanese.ts", "$dir.sub.French.vtt")

        assertTrue(CastFallback.canRestore(local, exists = { it == local.url }))
        assertTrue(CastFallback.canRestore(withTracks, exists = all::contains))
        // Deleted during the cast: the stream goes on
        assertFalse(CastFallback.canRestore(local, exists = { false }))
        assertFalse(CastFallback.canRestore(withTracks, exists = (all - "$dir.audio.Japanese.ts")::contains))
        assertFalse(CastFallback.canRestore(withTracks, exists = (all - "$dir.sub.French.vtt")::contains))
    }

    @Test
    fun filesThePhoneServedAreReplacedByTheSourceOnceGone() {
        // Cast from the phone, found running when the player opened or switched to from the TV
        val served = StreamSource(url = "http://192.168.1.18:8214/local-0123abcd-video.m3u8", fullName = "Kwik - 720p")
        assertTrue(CastFallback.needsOnlineStream(served))
        // A stream of the source goes on
        assertFalse(CastFallback.needsOnlineStream(online))
        assertFalse(CastFallback.needsOnlineStream(StreamSource(url = "http://192.168.1.18:8214/proxy?url=x")))
        assertFalse(CastFallback.needsOnlineStream(null))
    }
}
