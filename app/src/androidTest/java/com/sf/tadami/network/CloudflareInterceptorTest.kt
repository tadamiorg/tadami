package com.sf.tadami.network

import android.webkit.CookieManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.sf.tadami.network.interceptors.CloudflareInterceptor
import com.sf.tadami.network.interceptors.UserAgentInterceptor
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Drives the real WebView against a local fake Cloudflare challenge: the challenge page loads
 * `/solve.js`, which sets `cf_clearance` and reloads, exactly like a non-interactive challenge.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CloudflareInterceptorTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var server: MockWebServer
    private lateinit var cookieJar: AndroidCookieJar

    /** Every hit is one WebView actually running the challenge. */
    private val solveHits = AtomicInteger()
    private val clearedUserAgents = CopyOnWriteArrayList<String>()
    private val challengeUserAgents = CopyOnWriteArrayList<String>()

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { cookieJar = AndroidCookieJar() }
        clearAllCookies()
        server = MockWebServer()
        server.dispatcher = FakeCloudflare()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
        clearAllCookies()
    }

    @Test
    fun solvesChallengeAndReplaysRequest() {
        val response = client().newCall(GET(server.url("/anime").toString())).execute()

        response.use {
            assertEquals(200, it.code)
            assertEquals(SOLVED_BODY, it.body.string())
        }
        assertEquals(1, solveHits.get())
    }

    @Test
    fun solvesAChallengeServedAfterARedirectToAnotherHost() {
        // localhost redirects to 127.0.0.1: the challenge and its cookie belong to the latter
        val response = client(timeoutSeconds = 15)
            .newCall(GET("http://localhost:${server.port}/mirror")).execute()

        response.use {
            assertEquals(200, it.code)
            assertEquals(SOLVED_BODY, it.body.string())
            assertEquals("127.0.0.1", it.request.url.host)
        }
        assertEquals(1, solveHits.get())
    }

    @Test
    fun webViewAndOkHttpShareTheSameUserAgent() {
        client().newCall(GET(server.url("/anime").toString())).execute().close()

        // cf_clearance is bound to the user agent that solved the challenge
        assertTrue("WebView never hit the challenge", challengeUserAgents.isNotEmpty())
        assertTrue(challengeUserAgents.all { it == USER_AGENT })
        assertTrue(clearedUserAgents.isNotEmpty())
        assertTrue(clearedUserAgents.all { it == USER_AGENT })
    }

    @Test
    fun concurrentRequestsShareOneSolve() {
        val client = client()
        val pool = Executors.newFixedThreadPool(4)
        val codes = (0 until 4).map { i ->
            pool.submit<Int> {
                client.newCall(GET(server.url("/anime/$i").toString())).execute().use { it.code }
            }
        }.map { it.get(60, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(listOf(200, 200, 200, 200), codes)
        assertEquals("only one WebView should solve the challenge for a host", 1, solveHits.get())
    }

    @Test
    fun doesNotInterceptCloudflareBlockPages() {
        val start = System.nanoTime()
        val response = client().newCall(GET(server.url("/geo").toString())).execute()

        response.use { assertEquals(403, it.code) }
        assertEquals(0, solveHits.get())
        assertTrue(
            "a block page must not wait on a WebView",
            TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 5,
        )
    }

    @Test
    fun unsolvableChallengeFailsWithIOException() {
        try {
            client(timeoutSeconds = 5).newCall(GET(server.url("/never").toString())).execute().close()
            fail("expected the bypass to fail")
        } catch (e: IOException) {
            // expected
        }
    }

    private fun client(timeoutSeconds: Long = 20): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(UserAgentInterceptor(USER_AGENT))
            .addInterceptor(
                CloudflareInterceptor(context, cookieJar, USER_AGENT, timeoutSeconds = timeoutSeconds),
            )
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun clearAllCookies() {
        val latch = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            CookieManager.getInstance().removeAllCookies { latch.countDown() }
        }
        latch.await(5, TimeUnit.SECONDS)
    }

    private inner class FakeCloudflare : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.url.encodedPath
            val userAgent = request.headers["User-Agent"].orEmpty()
            val cleared = "cf_clearance=$TOKEN" in request.headers["Cookie"].orEmpty()
            return when {
                path == "/favicon.ico" -> MockResponse.Builder().code(404).build()
                path == "/mirror" -> MockResponse.Builder()
                    .code(302)
                    .addHeader("Location", "http://127.0.0.1:${server.port}/anime")
                    .build()
                path == "/solve.js" -> {
                    solveHits.incrementAndGet()
                    MockResponse.Builder()
                        .addHeader("Content-Type", "application/javascript")
                        .body(
                            "setTimeout(function () {" +
                                "document.cookie = 'cf_clearance=$TOKEN; path=/';" +
                                "location.reload();" +
                                "}, 300);",
                        )
                        .build()
                }
                path == "/geo" -> MockResponse.Builder()
                    .code(403)
                    .addHeader("Server", "cloudflare")
                    .addHeader("Content-Type", "text/html")
                    .body("<html><body><h1>Access denied</h1><span>Error code 1009</span></body></html>")
                    .build()
                cleared && path != "/never" -> {
                    clearedUserAgents += userAgent
                    MockResponse.Builder().body(SOLVED_BODY).build()
                }
                else -> {
                    challengeUserAgents += userAgent
                    val script = if (path == "/never") "" else "<script src=\"/solve.js\"></script>"
                    MockResponse.Builder()
                        .code(403)
                        .addHeader("Server", "cloudflare")
                        .addHeader("cf-mitigated", "challenge")
                        .addHeader("Content-Type", "text/html")
                        .body(
                            "<!DOCTYPE html><html><head><title>Just a moment...</title></head><body>" +
                                "<div id=\"challenge-error-title\">Enable JavaScript and cookies to continue</div>" +
                                script +
                                "</body></html>",
                        )
                        .build()
                }
            }
        }
    }

    private companion object {
        const val TOKEN = "fake-clearance-token"
        const val SOLVED_BODY = "past the challenge"
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
