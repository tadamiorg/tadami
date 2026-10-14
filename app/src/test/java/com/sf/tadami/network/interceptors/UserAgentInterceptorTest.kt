package com.sf.tadami.network.interceptors

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UserAgentInterceptorTest {

    private lateinit var server: MockWebServer

    @Volatile
    private var currentUserAgent = "Current"

    private val client = OkHttpClient.Builder()
        .addInterceptor(
            UserAgentInterceptor(
                userAgent = { currentUserAgent },
                replaces = { it == "Replaced" },
            ),
        )
        .build()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() = server.close()

    private fun sentUserAgent(requestUserAgent: String?): String? {
        server.enqueue(MockResponse.Builder().build())
        val request = Request.Builder().url(server.url("/")).apply {
            requestUserAgent?.let { header("User-Agent", it) }
        }.build()
        client.newCall(request).execute().close()
        return server.takeRequest().headers["User-Agent"]
    }

    @Test
    fun `adds the user agent read at each request`() {
        assertEquals("Current", sentUserAgent(null))
        currentUserAgent = "Changed"
        assertEquals("Changed", sentUserAgent(null))
    }

    @Test
    fun `replaces a replaced user agent only`() {
        assertEquals("Current", sentUserAgent("Replaced"))
        assertEquals("Custom", sentUserAgent("Custom"))
    }

    @Test
    fun `keeps the fixed user agent constructor`() {
        val fixedClient = OkHttpClient.Builder().addInterceptor(UserAgentInterceptor("Fixed")).build()
        server.enqueue(MockResponse.Builder().build())
        fixedClient.newCall(Request.Builder().url(server.url("/")).build()).execute().close()
        assertEquals("Fixed", server.takeRequest().headers["User-Agent"])
    }
}
