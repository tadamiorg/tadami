package com.sf.tadami.data.download.stream

import com.sf.tadami.data.download.FileProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResumePointTest {

    private val saved = FileProgress(name = "Ep 1.ts", hls = true, bytes = 4_000, segments = 4, total = 10)

    @Test
    fun anHlsDownloadGoesOnAfterItsWrittenSegments() {
        assertEquals(HlsDownload.ResumePoint(segments = 4, bytes = 4_000), HlsDownload.resumePoint(saved, 10, 4_000))
        // Half of the 5th segment was written when it stopped: cut back to the 4th
        assertEquals(HlsDownload.ResumePoint(segments = 4, bytes = 4_000), HlsDownload.resumePoint(saved, 10, 4_600))
    }

    @Test
    fun anHlsDownloadStartsOverWhenItCantGoOn() {
        // Nothing written yet
        assertNull(HlsDownload.resumePoint(null, 10, 0))
        assertNull(HlsDownload.resumePoint(saved.copy(segments = 0, bytes = 0), 10, 0))
        // Another playlist behind the stream now
        assertNull(HlsDownload.resumePoint(saved, 12, 4_000))
        // The file is gone or shorter than saved
        assertNull(HlsDownload.resumePoint(saved, 10, null))
        assertNull(HlsDownload.resumePoint(saved, 10, 3_999))
        // It was a progressive file
        assertNull(HlsDownload.resumePoint(saved.copy(hls = false), 10, 4_000))
    }

    @Test
    fun aCompleteDownloadHasNothingLeft() {
        val complete = saved.copy(segments = 10, bytes = 10_000)
        assertEquals(HlsDownload.ResumePoint(10, 10_000), HlsDownload.resumePoint(complete, 10, 10_000))
    }

    @Test
    fun theTotalOfAPartialResponseIsRead() {
        assertEquals(1000L, ProgressiveDownload.contentRangeTotal("bytes 100-999/1000"))
        assertNull(ProgressiveDownload.contentRangeTotal("bytes 100-999/*"))
        assertNull(ProgressiveDownload.contentRangeTotal(null))
    }
}
