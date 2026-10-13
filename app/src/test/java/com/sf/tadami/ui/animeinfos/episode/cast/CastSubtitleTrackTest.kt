package com.sf.tadami.ui.animeinfos.episode.cast

import com.sf.tadami.source.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CastSubtitleTrackTest {

    private val tracks = listOf(
        Track.SubtitleTrack("https://cdn/en.vtt", "English"),
        Track.SubtitleTrack("https://cdn/fr.vtt", "French"),
    )

    @Test
    fun theFirstPreferredLanguageIsTurnedOn() {
        assertEquals(2L, castSubtitleTrackId(tracks, listOf("fr", "en"), subtitlesEnabled = true))
        assertEquals(1L, castSubtitleTrackId(tracks, listOf("de", "en"), subtitlesEnabled = true))
    }

    @Test
    fun languagesMatchWhateverTheirCase() {
        val tracks = listOf(Track.SubtitleTrack("https://cdn/en.vtt", "en"), Track.SubtitleTrack("https://cdn/pt.vtt", "pt-BR"))
        assertEquals(2L, castSubtitleTrackId(tracks, listOf("PT-br"), subtitlesEnabled = true))
    }

    @Test
    fun withoutAMatchTheFirstTrackIsTurnedOn() {
        assertEquals(1L, castSubtitleTrackId(tracks, listOf("de"), subtitlesEnabled = true))
    }

    @Test
    fun noTrackIsTurnedOnWhenSubtitlesAreOff() {
        assertNull(castSubtitleTrackId(tracks, listOf("fr"), subtitlesEnabled = false))
        assertNull(castSubtitleTrackId(emptyList(), listOf("fr"), subtitlesEnabled = true))
    }
}
