package com.sf.tadami.data.download.stream

import com.sf.tadami.network.HttpException
import com.sf.tadami.source.model.StreamSource
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class StreamFallbackTest {

    private val kwik = StreamSource(url = "https://kwik/1.m3u8", fullName = "Kwik 1080p", quality = "1080p", server = "Kwik")
    private val sendvid = StreamSource(url = "https://sendvid/1.mp4", fullName = "Sendvid", server = "Sendvid")
    private val vidmoly = StreamSource(url = "https://vidmoly/1.m3u8", fullName = "Vidmoly 720p", quality = "720p", server = "Vidmoly")
    private val streams = listOf(kwik, sendvid, vidmoly)

    @Test
    fun aNewDownloadFollowsTheExtensionOrder() {
        assertEquals(streams, StreamFallback.attemptOrder(streams, resumedKey = null, failedKeys = emptyList()))
    }

    @Test
    fun aResumedDownloadGoesOnWithItsStreamFirst() {
        assertEquals(
            listOf(vidmoly, kwik, sendvid),
            StreamFallback.attemptOrder(streams, StreamFallback.key(vidmoly), emptyList()),
        )
    }

    @Test
    fun theStreamIsFoundAgainWhenItsUrlChanged() {
        // Fetched again after a process death, the URL signed again
        val refetched = listOf(kwik.copy(url = "https://kwik/1.m3u8?token=new"), sendvid, vidmoly)
        val order = StreamFallback.attemptOrder(refetched, StreamFallback.key(kwik), emptyList())
        assertEquals("https://kwik/1.m3u8?token=new", order.first().url)
    }

    @Test
    fun failedStreamsComeLast() {
        assertEquals(
            listOf(vidmoly, kwik, sendvid),
            StreamFallback.attemptOrder(streams, null, listOf(StreamFallback.key(kwik), StreamFallback.key(sendvid))),
        )
        // A failed stream isn't resumed first
        assertEquals(
            listOf(sendvid, vidmoly, kwik),
            StreamFallback.attemptOrder(streams, StreamFallback.key(kwik), listOf(StreamFallback.key(kwik))),
        )
    }

    @Test
    fun badStreamsMoveToTheNextOne() {
        assertEquals(FailureAction.NEXT_STREAM, StreamFallback.classify(HttpException(404), online = true))
        assertEquals(FailureAction.NEXT_STREAM, StreamFallback.classify(HttpException(503), online = true))
        assertEquals(FailureAction.NEXT_STREAM, StreamFallback.classify(StreamFailedException("Not a video"), online = true))
        assertEquals(FailureAction.NEXT_STREAM, StreamFallback.classify(SocketTimeoutException(), online = true))
        assertEquals(FailureAction.NEXT_STREAM, StreamFallback.classify(IllegalArgumentException("Bad URL"), online = true))
    }

    @Test
    fun networkErrorsWhileOfflineWaitForTheNetwork() {
        assertEquals(FailureAction.WAIT_FOR_NETWORK, StreamFallback.classify(IOException("unreachable"), online = false))
        assertEquals(
            FailureAction.WAIT_FOR_NETWORK,
            StreamFallback.classify(NetworkLostException(IOException()), online = true),
        )
        // A host answering an error isn't the network's fault
        assertEquals(FailureAction.NEXT_STREAM, StreamFallback.classify(HttpException(403), online = false))
    }

    @Test
    fun storageAndCancellationStop() {
        assertEquals(FailureAction.STOP, StreamFallback.classify(StorageException(IOException("ENOSPC")), online = true))
        assertEquals(FailureAction.STOP, StreamFallback.classify(DownloadFatalException("No stream"), online = true))
        assertEquals(FailureAction.STOP, StreamFallback.classify(CancellationException(), online = true))
    }

    @Test
    fun onlyTransientRequestErrorsAreRetried() {
        assertTrue(StreamFallback.isRetryable(IOException()))
        assertTrue(StreamFallback.isRetryable(HttpException(503)))
        assertTrue(StreamFallback.isRetryable(HttpException(429)))
        assertTrue(StreamFallback.isRetryable(HttpException(408)))
        assertFalse(StreamFallback.isRetryable(HttpException(404)))
        assertFalse(StreamFallback.isRetryable(HttpException(403)))
        assertFalse(StreamFallback.isRetryable(StreamFailedException("Not a video")))
        assertFalse(StreamFallback.isRetryable(NetworkLostException(IOException())))
    }
}
