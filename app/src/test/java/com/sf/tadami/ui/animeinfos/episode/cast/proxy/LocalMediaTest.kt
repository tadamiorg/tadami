package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import com.sf.tadami.data.download.SegmentMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMediaTest {

    private val segments = listOf(
        SegmentMetadata(offset = 0, length = 1_000, durationUs = 10_010_000),
        SegmentMetadata(offset = 1_000, length = 1_200, durationUs = 10_010_000, discontinuity = true),
        SegmentMetadata(offset = 2_200, length = 300, durationUs = 4_004_000),
    )

    @Test
    fun theSegmentsAreByteRangesOfTheJoinedFile() {
        assertEquals(
            """
            #EXTM3U
            #EXT-X-VERSION:4
            #EXT-X-TARGETDURATION:11
            #EXT-X-MEDIA-SEQUENCE:0
            #EXT-X-PLAYLIST-TYPE:VOD
            #EXTINF:10.010000,
            #EXT-X-BYTERANGE:1000@0
            local-t-video.ts
            #EXT-X-DISCONTINUITY
            #EXTINF:10.010000,
            #EXT-X-BYTERANGE:1200@1000
            local-t-video.ts
            #EXTINF:4.004000,
            #EXT-X-BYTERANGE:300@2200
            local-t-video.ts
            #EXT-X-ENDLIST

            """.trimIndent(),
            LocalPlaylists.media("local-t-video.ts", segments),
        )
    }

    @Test
    fun anEmptySegmentIsAGap() {
        val playlist = LocalPlaylists.media("v.ts", listOf(segments[0], SegmentMetadata(1_000, 0, 2_000_000), segments[1]))
        assertTrue(playlist, playlist.contains("#EXTINF:2.000000,\n#EXT-X-GAP\nv.ts\n"))
        assertTrue(playlist, playlist.contains("#EXT-X-BYTERANGE:1200@1000\n"))
    }

    @Test
    fun theMasterHasTheAudioAsARendition() {
        assertEquals(
            """
            #EXTM3U
            #EXT-X-VERSION:4
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="Japanese",DEFAULT=YES,AUTOSELECT=YES,URI="local-t-audio0.m3u8"
            #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="audio",NAME="English",DEFAULT=NO,AUTOSELECT=YES,URI="local-t-audio1.m3u8"
            #EXT-X-STREAM-INF:BANDWIDTH=959,AUDIO="audio"
            local-t-video.m3u8

            """.trimIndent(),
            LocalPlaylists.master(
                videoUri = "local-t-video.m3u8",
                bandwidth = LocalPlaylists.peakBitrate(segments),
                audios = listOf("local-t-audio0.m3u8" to "Japanese", "local-t-audio1.m3u8" to "Eng\"lish"),
            ),
        )
    }

    @Test
    fun aRangeIsReadAsHttpSaysOrIgnored() {
        assertEquals(ByteRange.Whole, ByteRange.parse(null, 100))
        assertEquals(ByteRange.Part(0, 99), ByteRange.parse("bytes=0-", 100))
        assertEquals(ByteRange.Part(10, 19), ByteRange.parse("bytes=10-19", 100))
        // Past the end: up to the end
        assertEquals(ByteRange.Part(90, 99), ByteRange.parse("bytes=90-500", 100))
        // The last bytes
        assertEquals(ByteRange.Part(80, 99), ByteRange.parse("bytes=-20", 100))
        assertEquals(ByteRange.Part(0, 99), ByteRange.parse("bytes=-500", 100))
        assertEquals(ByteRange.Unsatisfiable, ByteRange.parse("bytes=100-", 100))
        assertEquals(ByteRange.Unsatisfiable, ByteRange.parse("bytes=-0", 100))
        // Another unit, several ranges, malformed: the whole file
        assertEquals(ByteRange.Whole, ByteRange.parse("items=0-1", 100))
        assertEquals(ByteRange.Whole, ByteRange.parse("bytes=0-1,5-6", 100))
        assertEquals(ByteRange.Whole, ByteRange.parse("bytes=5-3", 100))
        assertEquals(ByteRange.Whole, ByteRange.parse("bytes=a-b", 100))
    }
}
