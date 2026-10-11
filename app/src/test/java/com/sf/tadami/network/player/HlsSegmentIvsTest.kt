package com.sf.tadami.network.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HlsSegmentIvsTest {

    @Test
    fun segmentsAfterAGapGetTheirEncoderNumberAsIv() {
        // Kwik: segment-2 was dropped, the files keep the numbers they were encrypted with
        val fixed = HlsSegmentIvs.fix(kwikPlaylist(1, 3, 4, 5))
        assertNotNull(fixed)
        fixed!!

        assertEquals(
            listOf(1L, 3L, 4L, 5L),
            ivsBySegment(fixed).map { (_, iv) -> iv },
        )
        assertEquals(
            listOf("segment-1-v1-a1.jpg", "segment-3-v1-a1.jpg", "segment-4-v1-a1.jpg", "segment-5-v1-a1.jpg"),
            ivsBySegment(fixed).map { (uri, _) -> uri.substringAfterLast('/') },
        )
        // Everything else is kept
        assertEquals(
            kwikPlaylist(1, 3, 4, 5).lines().filterNot { it.startsWith("#EXT-X-KEY") },
            fixed.lines().filterNot { it.startsWith("#EXT-X-KEY") },
        )
    }

    @Test
    fun playlistsWithoutGapsAreLeftAlone() {
        assertNull(HlsSegmentIvs.fix(kwikPlaylist(1, 2, 3, 4)))
    }

    @Test
    fun namesNotFollowingTheSequenceAreLeftAlone() {
        // Numbered from 0 while the sequence starts at 1: nothing says the names are the IVs
        assertNull(HlsSegmentIvs.fix(kwikPlaylist(0, 2, 3)))
        // Not increasing
        assertNull(HlsSegmentIvs.fix(kwikPlaylist(1, 3, 2)))
    }

    @Test
    fun explicitIvsAndOtherMethodsAreLeftAlone() {
        val explicit = kwikPlaylist(1, 3, 4).replace(
            "#EXT-X-KEY:METHOD=AES-128,URI=\"https://vault-99.owocdn.top/stream/99/02/abc/mon.key\"",
            "#EXT-X-KEY:METHOD=AES-128,URI=\"https://vault-99.owocdn.top/stream/99/02/abc/mon.key\",IV=0x0000000000000000000000000000000A",
        )
        assertNull(HlsSegmentIvs.fix(explicit))
        assertNull(HlsSegmentIvs.fix(kwikPlaylist(1, 3, 4).replace("METHOD=AES-128", "METHOD=SAMPLE-AES")))
        assertNull(HlsSegmentIvs.fix(kwikPlaylist(1, 3, 4).lines().filterNot { it.startsWith("#EXT-X-KEY") }.joinToString("\n")))
    }

    @Test
    fun multivariantPlaylistsAreLeftAlone() {
        val multivariant = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=1280x720
            720p/index.m3u8
        """.trimIndent()
        assertNull(HlsSegmentIvs.fix(multivariant))
    }

    private fun kwikPlaylist(vararg segmentNumbers: Int): String = buildString {
        appendLine("#EXTM3U")
        appendLine("#EXT-X-TARGETDURATION:18")
        appendLine("#EXT-X-ALLOW-CACHE:YES")
        appendLine("#EXT-X-PLAYLIST-TYPE:VOD")
        appendLine("#EXT-X-KEY:METHOD=AES-128,URI=\"https://vault-99.owocdn.top/stream/99/02/abc/mon.key\"")
        appendLine("#EXT-X-VERSION:3")
        appendLine("#EXT-X-MEDIA-SEQUENCE:1")
        segmentNumbers.forEach {
            appendLine("#EXTINF:4.838,")
            appendLine("https://vault-99.owocdn.top/stream/99/02/abc/segment-$it-v1-a1.jpg")
        }
        append("#EXT-X-ENDLIST")
    }

    // Each segment URI with the IV of the key line in effect for it
    private fun ivsBySegment(playlist: String): List<Pair<String, Long>> {
        var iv: Long? = null
        return playlist.lines().mapNotNull { line ->
            when {
                line.startsWith("#EXT-X-KEY") ->
                    iv = line.substringAfter("IV=0x", "").takeIf { it.isNotEmpty() }?.toLong(16)
                line.isNotBlank() && !line.startsWith("#") -> return@mapNotNull line to iv!!
            }
            null
        }
    }
}
