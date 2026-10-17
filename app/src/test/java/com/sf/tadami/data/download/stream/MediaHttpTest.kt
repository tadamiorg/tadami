package com.sf.tadami.data.download.stream

import com.sf.tadami.network.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class MediaHttpTest {

    private lateinit var server: MockWebServer
    private val http = MediaHttp(OkHttpClient(), firstRetryDelayMs = 10)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun serverErrorsAreRetried() = runBlocking {
        server.enqueue(MockResponse.Builder().code(503).build())
        server.enqueue(MockResponse.Builder().code(429).build())
        server.enqueue(MockResponse.Builder().body("segment").build())

        assertEquals("segment", http.bytes(server.url("/seg-1.ts").toString(), null).decodeToString())
        assertEquals(3, server.requestCount)
    }

    @Test
    fun aMissingFileIsNotRetried() = runBlocking {
        server.enqueue(MockResponse.Builder().code(404).build())
        try {
            http.bytes(server.url("/gone.mp4").toString(), null)
            fail("A 404 must fail")
        } catch (e: HttpException) {
            assertEquals(404, e.code)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun theStreamHeadersAndTheRangeAreSent() = runBlocking {
        server.enqueue(MockResponse.Builder().code(206).body("rest").build())
        val headers = Headers.headersOf("Referer", "https://kwik.cx/")

        http.bytes(server.url("/video.mp4").toString(), headers, range = "bytes=100-")

        val request = server.takeRequest()
        assertEquals("https://kwik.cx/", request.headers["Referer"])
        assertEquals("bytes=100-", request.headers["Range"])
        // Offsets are about the raw bytes
        assertTrue(request.headers["Accept-Encoding"] == "identity")
    }

    @Test
    fun aReadStoppedByACancellationEndsAsACancellation() = runBlocking {
        // A pause cancels a read blocked on a slow host: it must not look like a failed stream
        server.enqueue(
            MockResponse.Builder()
                .body("x".repeat(64 * 1024))
                .throttleBody(1024, 1, TimeUnit.SECONDS)
                .build(),
        )
        var thrown: Throwable? = null
        val job = launch(Dispatchers.IO) {
            try {
                http.bytes(server.url("/slow.mp4").toString(), null)
            } catch (e: Throwable) {
                thrown = e
            }
        }
        delay(500)
        job.cancelAndJoin()

        assertTrue("Got $thrown", thrown is CancellationException)
    }
}
