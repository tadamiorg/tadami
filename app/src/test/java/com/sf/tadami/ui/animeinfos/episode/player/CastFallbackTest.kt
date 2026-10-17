package com.sf.tadami.ui.animeinfos.episode.player

import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.animeinfos.episode.player.CastFallback.AfterCast
import org.junit.Assert.assertEquals
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
}
