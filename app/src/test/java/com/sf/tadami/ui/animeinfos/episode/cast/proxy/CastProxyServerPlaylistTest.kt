package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import android.content.ContextWrapper
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import okio.GzipSink
import okio.buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URLEncoder

/** The real proxy between a client (the web receiver) and a fake Kwik CDN. */
class CastProxyServerPlaylistTest {

    private lateinit var upstream: MockWebServer
    private lateinit var proxy: CastProxyServer
    private val client = OkHttpClient()

    @Before
    fun setUp() {
        upstream = MockWebServer()
        upstream.dispatcher = FakeKwik()
        upstream.start()
        proxy = CastProxyServer(ContextWrapper(null), OkHttpClient())
        assertTrue(proxy.startSafely())
    }

    @After
    fun tearDown() {
        proxy.stop()
        upstream.close()
    }

    @Test
    fun playlistsWithAnIvGapAreServedWithExplicitIvs() {
        get("/stream/abc/uwu.m3u8").use { response ->
            assertEquals(200, response.code)
            val body = response.body.string()
            // segment-3 sits second in the playlist: its IV must be its own number, not 2
            assertTrue(body, body.contains("IV=0x00000000000000000000000000000003\nhttp"))
            assertTrue(body.contains("segment-3-v1-a1.jpg"))
            assertEquals("application/vnd.apple.mpegurl", response.header("Content-Type"))
            assertNull("the original body's validator", response.header("ETag"))
            assertEquals("*", response.header("Access-Control-Allow-Origin"))
        }
    }

    @Test
    fun playlistsThatDontNeedItAreForwardedAsIs() {
        get("/aligned/uwu.m3u8").use { response ->
            assertEquals(ALIGNED_PLAYLIST, response.body.string())
            assertNotNull(response.header("ETag"))
        }
    }

    @Test
    fun playlistsAreRecognisedByTheirBytesNotTheirUrlOrType() {
        get("/playlist?id=4").use { response ->
            val body = response.body.string()
            assertTrue(body, body.contains("IV=0x00000000000000000000000000000003\nhttp"))
        }
        // A segment labelled as a playlist is still a segment
        get("/stream/abc/mislabelled-segment").use { response ->
            assertArrayEquals(SEGMENT, response.body.bytes())
        }
    }

    @Test
    fun segmentsStreamThroughUntouched() {
        get("/stream/abc/segment-3-v1-a1.jpg").use { response ->
            assertEquals(200, response.code)
            assertArrayEquals(SEGMENT, response.body.bytes())
            assertEquals("image/jpeg", response.header("Content-Type"))
        }
    }

    @Test
    fun upstreamErrorsAreForwarded() {
        get("/stream/abc/uwu.m3u8", referer = null).use { response ->
            assertEquals(403, response.code)
            assertEquals("forbidden", response.body.string())
        }
    }

    private fun get(path: String, referer: String? = "https://kwik.cx/"): okhttp3.Response {
        val target = URLEncoder.encode(upstream.url(path).toString(), "UTF-8")
        val headers = referer?.let { URLEncoder.encode("""{"Referer":"$it"}""", "UTF-8") }
        val url = "http://127.0.0.1:${proxy.listeningPort}/proxy?url=$target" +
            (headers?.let { "&headers=$it" } ?: "")
        // Like a browser, the client asks for compressed responses (OkHttp adds Accept-Encoding: gzip)
        return client.newCall(Request.Builder().url(url).build()).execute()
    }

    /** Needs the referer like Kwik, and compresses whenever asked to. */
    private inner class FakeKwik : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            if (request.headers["Referer"] != "https://kwik.cx/") {
                return MockResponse.Builder().code(403).body("forbidden").build()
            }
            val gzip = "gzip" in request.headers["Accept-Encoding"].orEmpty()
            return when (request.url.encodedPath) {
                "/stream/abc/uwu.m3u8" -> playlist(gapPlaylist(), gzip)
                "/aligned/uwu.m3u8" -> playlist(ALIGNED_PLAYLIST, gzip)
                // No playlist extension, so the receiver's Accept-Encoding reaches it: served plain
                "/playlist" -> MockResponse.Builder()
                    .addHeader("Content-Type", "application/octet-stream")
                    .body(gapPlaylist())
                    .build()
                "/stream/abc/mislabelled-segment" -> MockResponse.Builder()
                    .addHeader("Content-Type", "application/vnd.apple.mpegurl")
                    .body(Buffer().write(SEGMENT))
                    .build()
                "/stream/abc/segment-3-v1-a1.jpg" -> MockResponse.Builder()
                    .addHeader("Content-Type", "image/jpeg")
                    .body(Buffer().write(SEGMENT))
                    .build()
                else -> MockResponse.Builder().code(404).build()
            }
        }

        private fun playlist(text: String, gzip: Boolean): MockResponse {
            val builder = MockResponse.Builder()
                .addHeader("Content-Type", "application/vnd.apple.mpegurl")
                .addHeader("ETag", "\"v1\"")
            return if (gzip) {
                val compressed = Buffer()
                GzipSink(compressed).buffer().use { it.writeUtf8(text) }
                builder.addHeader("Content-Encoding", "gzip").body(compressed).build()
            } else {
                builder.body(text).build()
            }
        }

        private fun gapPlaylist() = kwikPlaylist(upstream.url("/stream/abc/").toString(), 1, 3, 4)
    }

    private companion object {
        val SEGMENT = ByteArray(4096) { (it * 31).toByte() }

        fun kwikPlaylist(base: String, vararg numbers: Int) = buildString {
            append("#EXTM3U\n#EXT-X-TARGETDURATION:18\n#EXT-X-PLAYLIST-TYPE:VOD\n")
            append("#EXT-X-KEY:METHOD=AES-128,URI=\"${base}mon.key\"\n")
            append("#EXT-X-VERSION:3\n#EXT-X-MEDIA-SEQUENCE:1\n")
            numbers.forEach { append("#EXTINF:4.838,\n${base}segment-$it-v1-a1.jpg\n") }
            append("#EXT-X-ENDLIST")
        }

        val ALIGNED_PLAYLIST = kwikPlaylist("https://cdn.example/aligned/", 1, 2, 3)
    }
}
