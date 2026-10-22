package com.sf.tadami.ui.animeinfos.episode.cast.preview

import android.os.SystemClock
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.sf.tadami.network.player.PlayerNetworkHelper
import com.sf.tadami.ui.animeinfos.episode.player.preview.PREVIEW_FETCH_TIMEOUT_MS
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * The cast seekbar preview against a local server whose segments never come: the request carries the
 * source's headers to the stream, the step is given up after the phone player's 10 s, answered
 * "none", and never decoded again.
 */
@UnstableApi
@LargeTest
@RunWith(AndroidJUnit4::class)
class CastSeekPreviewTimeoutTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val replies = LinkedBlockingQueue<String>()
    private val requests = CopyOnWriteArrayList<RecordedRequest>()
    private lateinit var server: MockWebServer
    private lateinit var preview: CastSeekPreview

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                return if (request.url.encodedPath == "/media.m3u8") {
                    MockResponse.Builder().addHeader("Content-Type", "application/vnd.apple.mpegurl").body(PLAYLIST).build()
                } else {
                    // A segment that hangs
                    MockResponse.Builder().headersDelay(60, TimeUnit.SECONDS).body("").build()
                }
            }
        }
        server.start()
        instrumentation.runOnMainSync {
            preview = CastSeekPreview(
                context = context,
                playerNetworkHelper = Injekt.get<PlayerNetworkHelper>(),
                send = { replies.put(it) },
                localFileUri = { null },
                receiverCapable = { true },
                isOnWifi = { true },
            )
            preview.settings = CastSeekPreview.Settings(enabled = true, onlyOnWifi = false, userAgent = "fallback-ua")
        }
    }

    @After
    fun tearDown() {
        instrumentation.runOnMainSync { preview.destroy() }
        server.close()
    }

    private fun ask(step: Int, headers: Map<String, String>?): Pair<String, Long> {
        val request = CastPreviewRequest(
            type = CastPreviewProtocol.TYPE_REQUEST,
            requestId = step.toLong(),
            sourceId = "1|${server.url("/media.m3u8")}|local|test",
            url = server.url("/media.m3u8").toString(),
            headers = headers,
            step = step,
            duration = 100_000,
        )
        val started = SystemClock.elapsedRealtime()
        instrumentation.runOnMainSync { preview.onRequest(request) }
        val message = replies.poll(30, TimeUnit.SECONDS) ?: throw AssertionError("no reply for step $step")
        val status = Json.parseToJsonElement(message).jsonObject["status"]!!.jsonPrimitive.content
        return status to SystemClock.elapsedRealtime() - started
    }

    @Test
    fun aStepWithoutFrameInTimeIsNoneAndNotDecodedAgain() {
        val (status, elapsed) = ask(3, mapOf("Referer" to "https://tadami.example/"))
        assertEquals(CastPreviewProtocol.STATUS_NONE, status)
        assertTrue("given up after $elapsed ms", elapsed >= PREVIEW_FETCH_TIMEOUT_MS && elapsed < PREVIEW_FETCH_TIMEOUT_MS + 5_000)

        // The stream was read with the source's headers
        val playlist = requests.first { it.url.encodedPath == "/media.m3u8" }
        assertEquals("https://tadami.example/", playlist.headers["Referer"])

        val seen = requests.size
        val (again, againElapsed) = ask(3, mapOf("Referer" to "https://tadami.example/"))
        assertEquals(CastPreviewProtocol.STATUS_NONE, again)
        assertTrue("answered at once, $againElapsed ms", againElapsed < 500)
        assertEquals("nothing fetched again", seen, requests.size)
    }

    @Test
    fun aSourceWithoutHeadersGetsTheUserAgentSetting() {
        ask(1, null)
        val playlist = requests.first { it.url.encodedPath == "/media.m3u8" }
        assertEquals("fallback-ua", playlist.headers["User-Agent"])
    }

    private companion object {
        val PLAYLIST = buildString {
            appendLine("#EXTM3U")
            appendLine("#EXT-X-VERSION:3")
            appendLine("#EXT-X-TARGETDURATION:10")
            appendLine("#EXT-X-MEDIA-SEQUENCE:0")
            appendLine("#EXT-X-PLAYLIST-TYPE:VOD")
            repeat(10) {
                appendLine("#EXTINF:10.0,")
                appendLine("seg$it.ts")
            }
            appendLine("#EXT-X-ENDLIST")
        }
    }
}
