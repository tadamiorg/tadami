package com.sf.tadami.network

import android.webkit.CookieManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Headers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class WebViewResolverTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var server: MockWebServer
    private lateinit var cookieJar: AndroidCookieJar
    private lateinit var resolver: WebViewResolver
    private val userAgents = CopyOnWriteArrayList<String>()

    @Before
    fun setUp() {
        instrumentation.runOnMainSync { cookieJar = AndroidCookieJar() }
        clearAllCookies()
        resolver = WebViewResolver(instrumentation.targetContext, cookieJar) { USER_AGENT }
        server = MockWebServer()
        server.dispatcher = Pages()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
        clearAllCookies()
    }

    @Test
    fun scriptCanWaitForContentAddedLater() {
        val result = resolver.load(
            url = server.url("/player").toString(),
            script = """
                new Promise(resolve => {
                  const timer = setInterval(() => {
                    const frame = document.querySelector('iframe');
                    if (frame) { clearInterval(timer); resolve(frame.src); }
                  }, 100);
                })
            """.trimIndent(),
            timeoutSeconds = 20,
        )

        assertFalse(result.timedOut)
        assertEquals("https://player.example/embed/42", result.scriptResult)
        assertTrue(result.html!!.contains("<iframe"))
    }

    @Test
    fun nonStringScriptResultsAreJson() {
        val result = resolver.load(
            url = server.url("/player").toString(),
            script = "({ id: 42, ok: true })",
            timeoutSeconds = 20,
        )

        assertEquals("""{"id":42,"ok":true}""", result.scriptResult)
    }

    @Test
    fun interceptsRequestsMadeByThePage() {
        val result = resolver.load(
            url = server.url("/stream").toString(),
            interceptRequest = { it.endsWith(".m3u8") },
            timeoutSeconds = 20,
        )

        assertFalse(result.timedOut)
        assertEquals(server.url("/media/master.m3u8").toString(), result.interceptedUrl)
    }

    @Test
    fun returnsTheDomBuiltByJavaScript() {
        val result = resolver.load(url = server.url("/generated").toString(), timeoutSeconds = 20)

        assertFalse(result.timedOut)
        assertTrue(result.html!!.contains("built by javascript"))
        assertTrue("UA must be the app's", userAgents.isNotEmpty() && userAgents.all { it == USER_AGENT })
    }

    @Test
    fun headerUserAgentWins() {
        val custom = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Mobile Safari/537.36"
        resolver.load(
            url = server.url("/generated").toString(),
            headers = Headers.headersOf("User-Agent", custom),
            timeoutSeconds = 20,
        )

        assertTrue(userAgents.isNotEmpty() && userAgents.all { it == custom })
    }

    @Test
    fun waitsThroughACloudflareChallengeAndSharesTheClearance() {
        val url = server.url("/protected")
        val result = resolver.load(url = url.toString(), script = "document.title", timeoutSeconds = 20)

        assertFalse(result.timedOut)
        assertEquals("Real page", result.scriptResult)
        // OkHttp now gets the clearance through the shared cookie jar
        assertNotNull(cookieJar.get(url).firstOrNull { it.name == "cf_clearance" })
    }

    @Test
    fun timesOutWhenTheScriptNeverSettles() {
        val start = System.nanoTime()
        val result = resolver.load(
            url = server.url("/generated").toString(),
            script = "new Promise(() => {})",
            timeoutSeconds = 3,
        )

        assertTrue(result.timedOut)
        assertNull(result.scriptResult)
        assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 10)
    }

    @Test
    fun refusesToBlockTheMainThread() {
        var error: Throwable? = null
        instrumentation.runOnMainSync {
            try {
                resolver.load(server.url("/generated").toString())
            } catch (e: IllegalStateException) {
                error = e
            }
        }
        if (error == null) fail("load() must not run on the main thread")
    }

    private fun clearAllCookies() {
        val latch = CountDownLatch(1)
        instrumentation.runOnMainSync { CookieManager.getInstance().removeAllCookies { latch.countDown() } }
        latch.await(5, TimeUnit.SECONDS)
    }

    private inner class Pages : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.url.encodedPath
            if (path != "/favicon.ico") userAgents += request.headers["User-Agent"].orEmpty()
            val cleared = "cf_clearance=token" in request.headers["Cookie"].orEmpty()
            return when (path) {
                "/player" -> html(
                    "<html><body><script>" +
                        "setTimeout(function () {" +
                        "var f = document.createElement('iframe');" +
                        "f.src = 'https://player.example/embed/42';" +
                        "document.body.appendChild(f);" +
                        "}, 500);" +
                        "</script></body></html>",
                )
                "/stream" -> html(
                    "<html><body><script>" +
                        "setTimeout(function () { fetch('/media/master.m3u8'); }, 300);" +
                        "</script></body></html>",
                )
                "/media/master.m3u8" -> MockResponse.Builder()
                    .addHeader("Content-Type", "application/vnd.apple.mpegurl")
                    .body("#EXTM3U\n")
                    .build()
                "/generated" -> html(
                    "<html><body><script>" +
                        "document.body.innerHTML = '<p>built by javascript</p>';" +
                        "</script></body></html>",
                )
                "/protected" -> if (cleared) {
                    html("<html><head><title>Real page</title></head><body>content</body></html>")
                } else {
                    MockResponse.Builder()
                        .code(403)
                        .addHeader("Server", "cloudflare")
                        .addHeader("cf-mitigated", "challenge")
                        .addHeader("Content-Type", "text/html")
                        .body(
                            "<html><head><title>Just a moment...</title></head><body><script>" +
                                "window._cf_chl_opt = { cType: 'managed', cRay: '8f0e1d2c3b4a5968' };" +
                                "setTimeout(function () {" +
                                "document.cookie = 'cf_clearance=token; path=/';" +
                                "location.reload();" +
                                "}, 300);" +
                                "</script></body></html>",
                        )
                        .build()
                }
                else -> MockResponse.Builder().code(404).build()
            }
        }

        private fun html(body: String): MockResponse {
            return MockResponse.Builder().addHeader("Content-Type", "text/html").body(body).build()
        }
    }

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
