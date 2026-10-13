package com.sf.tadami.ui.animeinfos.episode.player.subtitles.webvtt

import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.webvtt.WebvttCueInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

@UnstableApi
class WebvttSubtitleTest {

    @Test
    fun cuesSharingAStartTimeGetTheirOwnLine() {
        val subtitle = WebvttSubtitle(
            listOf(
                cueInfo("Top", startUs = 1_000_000, endUs = 3_000_000),
                cueInfo("Bottom", startUs = 1_000_000, endUs = 2_000_000),
            )
        )

        val lines = subtitle.getCues(1_500_000).map { it.line }

        assertEquals(2, lines.size)
        assertNotEquals(lines[0], lines[1])
    }

    @Test
    fun aCueKeepsItsLineWhenTheCueSharingItsStartTimeEnds() {
        val subtitle = WebvttSubtitle(
            listOf(
                cueInfo("Long", startUs = 1_000_000, endUs = 3_000_000),
                cueInfo("Short", startUs = 1_000_000, endUs = 2_000_000),
            )
        )

        val before = subtitle.getCues(1_500_000).single { it.text == "Long" }.line
        val after = subtitle.getCues(2_500_000).single { it.text == "Long" }.line

        assertEquals(before, after)
    }

    private fun cueInfo(text: String, startUs: Long, endUs: Long) =
        WebvttCueInfo(Cue.Builder().setText(text).build(), startUs, endUs)
}
