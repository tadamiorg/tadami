package com.sf.tadami.data.download.stream

import com.sf.tadami.data.download.stream.HlsVariants.AudioRendition
import com.sf.tadami.data.download.stream.HlsVariants.Variant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HlsVariantsTest {

    private val v360 = Variant(index = 0, height = 360, bandwidth = 800_000, demuxedAudio = false)
    private val v720 = Variant(index = 1, height = 720, bandwidth = 2_500_000, demuxedAudio = false)
    private val v1080 = Variant(index = 2, height = 1080, bandwidth = 5_000_000, demuxedAudio = false)

    @Test
    fun qualityLabelsGiveAHeight() {
        assertEquals(1080, HlsVariants.targetHeight("1080p"))
        assertEquals(720, HlsVariants.targetHeight("Kwik 720p60"))
        assertEquals(1080, HlsVariants.targetHeight("1920x1080"))
        assertEquals(480, HlsVariants.targetHeight("Vidmoly - 480"))
        assertEquals(720, HlsVariants.targetHeight("HD720"))
        assertNull(HlsVariants.targetHeight("default"))
        assertNull(HlsVariants.targetHeight("Server 2"))
        // A year or an id isn't a height
        assertNull(HlsVariants.targetHeight("2024"))
    }

    @Test
    fun theVariantClosestToTheQualityWins() {
        val variants = listOf(v360, v720, v1080)
        assertEquals(v720, HlsVariants.chooseVariant(variants, "720p"))
        assertEquals(v1080, HlsVariants.chooseVariant(variants, "1080p"))
        // No exact one: the closest, the higher on a tie
        assertEquals(v720, HlsVariants.chooseVariant(variants, "640x576"))
        assertEquals(v1080, HlsVariants.chooseVariant(variants, "900p"))
    }

    @Test
    fun withoutAHeightTheHighestWins() {
        assertEquals(v1080, HlsVariants.chooseVariant(listOf(v360, v1080, v720), "Default"))
        // Unknown resolutions: the highest bandwidth
        val low = Variant(0, 0, 500_000, false)
        val high = Variant(1, 0, 3_000_000, false)
        assertEquals(high, HlsVariants.chooseVariant(listOf(low, high), "1080p"))
    }

    @Test
    fun muxedAudioIsPreferred() {
        val demuxed1080 = v1080.copy(demuxedAudio = true)
        assertEquals(v720, HlsVariants.chooseVariant(listOf(v360, v720, demuxed1080), "1080p"))
        // Only demuxed ones: the audio is downloaded aside
        val demuxed720 = v720.copy(demuxedAudio = true)
        assertEquals(demuxed1080, HlsVariants.chooseVariant(listOf(demuxed720, demuxed1080), ""))
    }

    @Test
    fun noVariantNoChoice() {
        assertNull(HlsVariants.chooseVariant(emptyList(), "1080p"))
    }

    @Test
    fun theDefaultAudioRenditionIsDownloaded() {
        val english = AudioRendition(index = 0, isDefault = false)
        val japanese = AudioRendition(index = 1, isDefault = true)
        assertEquals(japanese, HlsVariants.chooseAudio(listOf(english, japanese)))
        assertEquals(english, HlsVariants.chooseAudio(listOf(english, english.copy(index = 2))))
        assertNull(HlsVariants.chooseAudio(emptyList()))
    }
}
