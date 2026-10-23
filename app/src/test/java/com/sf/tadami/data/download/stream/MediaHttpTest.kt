package com.sf.tadami.data.download.stream

import com.sf.tadami.network.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class MediaHttpTest {

    private lateinit var server: MockWebServer
    private val http = MediaHttp(OkHttpClient(), firstRetryDelayMs = 10, firstRateLimitDelayMs = 10)

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
    fun aRateLimitWaitsTheRetryAfterOfTheHost() = runBlocking {
        server.enqueue(MockResponse.Builder().code(429).setHeader("Retry-After", "1").build())
        server.enqueue(MockResponse.Builder().body("segment").build())

        val start = System.nanoTime()
        assertEquals("segment", http.bytes(server.url("/seg-1.ts").toString(), null).decodeToString())

        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) >= 1000)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun theOtherRequestsToARateLimitedHostWaitToo() = runBlocking {
        val arrivals = CopyOnWriteArrayList<Long>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                arrivals += System.nanoTime()
                return if (arrivals.size == 1) {
                    MockResponse.Builder().code(429).setHeader("Retry-After", "2").build()
                } else {
                    MockResponse.Builder().body("segment").build()
                }
            }
        }

        val first = async(Dispatchers.IO) { http.bytes(server.url("/seg-1.ts").toString(), null) }
        while (arrivals.isEmpty()) delay(10)
        // Its 429 read meanwhile
        delay(500)
        val second = async(Dispatchers.IO) { http.bytes(server.url("/seg-2.ts").toString(), null) }
        first.await()
        second.await()

        // The host asked everyone to wait, not only the request it answered
        assertEquals(3, arrivals.size)
        arrivals.drop(1).forEach { assertTrue(TimeUnit.NANOSECONDS.toMillis(it - arrivals.first()) >= 2000) }
    }

    @Test
    fun aRateLimitIsRetriedLongerThanOtherErrors() = runBlocking {
        repeat(4) { server.enqueue(MockResponse.Builder().code(429).build()) }
        server.enqueue(MockResponse.Builder().body("segment").build())

        assertEquals("segment", http.bytes(server.url("/seg-1.ts").toString(), null).decodeToString())
        assertEquals(5, server.requestCount)
    }

    @Test
    fun aRateLimitWithoutRetryAfterWaitsLongerEachTime() = runBlocking {
        val http = MediaHttp(OkHttpClient(), firstRetryDelayMs = 10, firstRateLimitDelayMs = 200)
        repeat(2) { server.enqueue(MockResponse.Builder().code(429).build()) }
        server.enqueue(MockResponse.Builder().body("segment").build())

        val start = System.nanoTime()
        http.bytes(server.url("/seg-1.ts").toString(), null)

        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) >= 200 + 400)
    }

    @Test
    fun anUnavailableHostIsNotHeldByItsRetryAfter() = runBlocking {
        server.enqueue(MockResponse.Builder().code(503).setHeader("Retry-After", "60").build())
        server.enqueue(MockResponse.Builder().body("segment").build())

        val start = System.nanoTime()
        http.bytes(server.url("/seg-1.ts").toString(), null)

        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 5000)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun aHostThatStaysRateLimitedFails() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse.Builder().code(429).build()
        }
        try {
            http.bytes(server.url("/seg-1.ts").toString(), null)
            fail("A host that keeps refusing must fail")
        } catch (e: HttpException) {
            assertEquals(429, e.code)
        }
        assertEquals(5, server.requestCount)
    }

    @Test
    fun theRetryAfterIsReadInSecondsOrAsADate() {
        val now = 1_700_000_000_000L
        fun retryAfter(value: String) = MediaHttp.retryAfterMs(Headers.headersOf("Retry-After", value), now)

        assertEquals(10_000L, retryAfter("10"))
        assertEquals(30_000L, retryAfter(httpDate(now + 30_000)))
        // Already passed
        assertEquals(0L, retryAfter(httpDate(now - 30_000)))
        // Not waited for longer than that
        assertEquals(MediaHttp.MAX_RETRY_AFTER_MS, retryAfter("86400"))
        assertNull(retryAfter("soon"))
        assertNull(MediaHttp.retryAfterMs(Headers.headersOf(), now))
    }

    private fun httpDate(ms: Long): String {
        return SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("GMT") }
            .format(Date(ms))
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
