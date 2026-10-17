package com.sf.tadami.data.download.stream

import com.sf.tadami.data.download.EpisodeMetadata
import com.sf.tadami.data.download.FileProgress
import com.sf.tadami.data.download.MediaFileMetadata
import com.sf.tadami.data.download.SegmentMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SegmentIndexTest {

    private val durations = listOf(10_010_000L, 10_010_000L, 4_004_000L)

    @Test
    fun segmentsFollowEachOtherInTheJoinedFile() {
        assertEquals(
            listOf(
                SegmentMetadata(offset = 0, length = 1_000, durationUs = 10_010_000),
                SegmentMetadata(offset = 1_000, length = 1_200, durationUs = 10_010_000),
                SegmentMetadata(offset = 2_200, length = 300, durationUs = 4_004_000),
            ),
            HlsDownload.segmentIndex(2_500, listOf(1_000, 1_200, 300), durations, listOf(0, 0, 0)),
        )
    }

    @Test
    fun theInitSegmentComesFirst() {
        // fMP4: 700 bytes of init segment before the first one
        val index = HlsDownload.segmentIndex(3_200, listOf(1_000, 1_200, 300), durations, listOf(0, 0, 0))!!
        assertEquals(listOf(700L, 1_700L, 2_900L), index.map { it.offset })
    }

    @Test
    fun aDiscontinuityIsKept() {
        val index = HlsDownload.segmentIndex(2_500, listOf(1_000, 1_200, 300), durations, listOf(0, 1, 1))!!
        assertEquals(listOf(false, true, false), index.map { it.discontinuity })
    }

    @Test
    fun anUnknownSizeLeavesNoIndex() {
        // Resumed from a download that didn't record them
        assertNull(HlsDownload.segmentIndex(2_500, null, durations, listOf(0, 0, 0)))
        // A segment missing
        assertNull(HlsDownload.segmentIndex(2_200, listOf(1_000, 1_200), durations, listOf(0, 0, 0)))
        // Bigger than the file
        assertNull(HlsDownload.segmentIndex(2_000, listOf(1_000, 1_200, 300), durations, listOf(0, 0, 0)))
    }

    @Test
    fun aResumeKeepsTheSizesOfTheWrittenSegments() {
        val saved = FileProgress("Ep 1.ts", hls = true, bytes = 2_200, segments = 2, total = 3, sizes = listOf(1_000, 1_200))
        val point = HlsDownload.resumePoint(saved, 3, 2_500)!!
        assertEquals(listOf(1_000L, 1_200L), HlsDownload.resumedSizes(saved, point))
        // Saved by a version that didn't record them
        assertNull(HlsDownload.resumedSizes(saved.copy(sizes = null), point))
    }

    @Test
    fun theSegmentIndexRoundTripsInTheSidecar() {
        val segments = HlsDownload.segmentIndex(2_500, listOf(1_000, 1_200, 300), durations, listOf(0, 1, 1))
        val metadata = EpisodeMetadata(
            version = EpisodeMetadata.VERSION,
            fullName = "Kwik - 720p",
            video = MediaFileMetadata("E.ts", "ts", segments),
            audios = listOf(MediaFileMetadata("E.audio.Japanese.ts", "ts", segments)),
        )
        assertEquals(metadata, EpisodeMetadata.decode(EpisodeMetadata.encode(metadata)))
        // A progressive file has none
        val progressive = metadata.copy(video = MediaFileMetadata("E.mp4", "mp4"), audios = emptyList())
        assertNull(EpisodeMetadata.decode(EpisodeMetadata.encode(progressive))!!.video.segments)
    }
}
