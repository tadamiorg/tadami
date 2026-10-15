package com.sf.tadami.ui.animeinfos.episode.player.subtitles.webvtt

import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@UnstableApi
class WebvttCssParserTest {

    @Before
    fun setUp() {
        // Media3 1.11 detects tests through Build.FINGERPRINT, which is null on the JVM and makes it throw
        ParsableByteArray.setShouldEnforceLimitOnLegacyMethods(true)
    }

    @Test
    fun styleBlockEndingAfterTheLastRuleIsParsed() {
        // The selector parse returns null once the last rule is consumed: that must end the loop, not throw
        val styles = WebvttCssParser().parseBlock(input("::cue { color: yellow }\n\n"))

        assertEquals(1, styles.size)
        assertEquals(0xFFFFFF00.toInt(), styles[0].fontColor)
    }

    @Test
    fun everyRuleOfTheBlockIsParsed() {
        val styles = WebvttCssParser().parseBlock(
            input("::cue { color: yellow }\n::cue(b) { font-weight: bold }\n\n")
        )

        assertEquals(2, styles.size)
    }

    private fun input(block: String) = ParsableByteArray(block.toByteArray(Charsets.UTF_8))
}
