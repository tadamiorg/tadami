package com.sf.tadami.data.download.stream

import com.sf.tadami.network.HttpException
import com.sf.tadami.source.model.StreamSource
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class StreamFallbackTest {

    private val kwik = StreamSource(url = "https://kwik/1.m3u8", fullName = "Kwik 1080p", quality = "1080p", server = "Kwik")
    private val sendvid = StreamSource(url = "https://sendvid/1.mp4", fullName = "Sendvid", server = "Sendvid")
    private val vidmoly = StreamSource(url = "https://vidmoly/1.m3u8", fullName = "Vidmoly 720p", quality = "720p", server = "Vidmoly")
    private val streams = listOf(kwik, sendvid, vidmoly)

    @Test
    fun aNewDownloadTakesTheHighestQualityFirst() {
        // Sendvid states no quality: after those that do
        assertEquals(
            listOf(kwik, vidmoly, sendvid),
            StreamFallback.attemptOrder(streams, resumedKey = null, failedKeys = emptyList()),
        )
        // The extension's first stream is only 480p: the 1080p one of the next server goes first
        val first480 = StreamSource(url = "https://a/480.mp4", fullName = "Sibnet", quality = "480p", server = "Sibnet")
        val second1080 = StreamSource(url = "https://b/1080.m3u8", fullName = "Vidmoly", quality = "1080p", server = "Vidmoly")
        assertEquals(
            listOf(second1080, first480),
            StreamFallback.attemptOrder(listOf(first480, second1080), null, emptyList()),
        )
    }

    @Test
    fun equalQualitiesKeepTheExtensionOrder() {
        val sibnet = StreamSource(url = "https://sibnet/1.mp4", fullName = "Sibnet 720p", quality = "720p", server = "Sibnet")
        val sendvid720 = sendvid.copy(quality = "720p")
        assertEquals(
            listOf(sibnet, vidmoly, sendvid720),
            StreamFallback.attemptOrder(listOf(sibnet, vidmoly, sendvid720), null, emptyList()),
        )
    }

    @Test
    fun theQualityIsReadFromTheNameWhenTheLabelStatesNone() {
        val named = StreamSource(url = "https://lpayer/1.m3u8", fullName = "LPayer - 1080p", server = "LPayer")
        assertEquals(1080, StreamFallback.height(named))
        assertEquals(null, StreamFallback.height(sendvid))
        assertEquals(listOf(named, vidmoly), StreamFallback.attemptOrder(listOf(vidmoly, named), null, emptyList()))
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
            listOf(vidmoly, sendvid, kwik),
            StreamFallback.attemptOrder(streams, StreamFallback.key(kwik), listOf(StreamFallback.key(kwik))),
        )
    }

    @Test
    fun badStreamsMoveToTheNextOne() {
        assertEquals(FailureAction.NEXT_STREAM, StreamFallback.classify(HttpException(404), online = true))
        assertEquals(FailureAction.NEXT_STREAM, StreamFallback.classify(HttpException(503), online = true))
        assertEquals(FailureAction.NEXT_STREAM, StreamFallback.classify(StreamFailedException(StreamFailure.NOT_VIDEO, "Not a video"), online = true))
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
        assertFalse(StreamFallback.isRetryable(StreamFailedException(StreamFailure.NOT_VIDEO, "Not a video")))
        assertFalse(StreamFallback.isRetryable(NetworkLostException(IOException())))
    }

    @Test
    fun theErrorShownIsTheOneCallAwaitWrapped() {
        val unknownHost = UnknownHostException("Unable to resolve host \"dead.invalid\"")
        assertSame(unknownHost, StreamFallback.unwrap(IOException(unknownHost.message, unknownHost)))
        val timeout = SocketTimeoutException("timeout")
        assertSame(timeout, StreamFallback.unwrap(IOException(timeout.message, timeout)))
        // Not a wrapper: kept as it is
        val plain = IOException("Can't rename")
        assertSame(plain, StreamFallback.unwrap(plain))
        val lost = NetworkLostException(IOException("offline"))
        assertSame(lost, StreamFallback.unwrap(lost))
        val http = HttpException(404)
        assertSame(http, StreamFallback.unwrap(http))
    }
}
