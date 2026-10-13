package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import android.content.ContextWrapper
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URLEncoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** A long upstream response (a streaming video) must not hold up the proxy's other requests. */
class CastProxyServerConcurrencyTest {

    private lateinit var upstream: MockWebServer
    private lateinit var proxy: CastProxyServer
    private val upstreamStarted = CountDownLatch(1)
    private val releaseUpstream = CountDownLatch(1)
    private val client = OkHttpClient.Builder().readTimeout(10, TimeUnit.SECONDS).build()

    @Before
    fun setUp() {
        upstream = MockWebServer()
        upstream.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                upstreamStarted.countDown()
                releaseUpstream.await(10, TimeUnit.SECONDS)
                return MockResponse.Builder().body("video").build()
            }
        }
        upstream.start()
        proxy = CastProxyServer(ContextWrapper(null), OkHttpClient())
        assertTrue(proxy.startSafely())
    }

    @After
    fun tearDown() {
        releaseUpstream.countDown()
        proxy.stop()
        upstream.close()
    }

    @Test
    fun otherRequestsAreServedWhileAnUpstreamResponseIsPending() {
        val target = URLEncoder.encode(upstream.url("/video.mp4").toString(), "UTF-8")
        val slow = thread { get("/proxy?url=$target").close() }
        assertTrue(upstreamStarted.await(5, TimeUnit.SECONDS))

        val healthClient = client.newBuilder().callTimeout(3, TimeUnit.SECONDS).build()
        val health = healthClient.newCall(request("/health")).execute()
        health.use { assertEquals("ok", it.body.string()) }

        releaseUpstream.countDown()
        slow.join(5000)
    }

    private fun request(path: String) =
        Request.Builder().url("http://127.0.0.1:${proxy.listeningPort}$path").build()

    private fun get(path: String) = client.newCall(request(path)).execute()
}
