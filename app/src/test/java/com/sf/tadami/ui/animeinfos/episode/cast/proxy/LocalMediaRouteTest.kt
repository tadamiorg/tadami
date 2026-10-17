package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import android.content.ContextWrapper
import com.sf.tadami.data.download.SegmentMetadata
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.net.URLEncoder

/** The downloaded episode being cast, read by a client (the receiver) from the real proxy. */
class LocalMediaRouteTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var proxy: CastProxyServer
    private lateinit var video: File
    private lateinit var secret: File
    private val opened = mutableListOf<String>()
    private val client = OkHttpClient()
    private val bytes = ByteArray(2_500) { (it % 251).toByte() }
    private val segments = listOf(
        SegmentMetadata(offset = 0, length = 1_000, durationUs = 10_000_000),
        SegmentMetadata(offset = 1_000, length = 1_500, durationUs = 8_000_000),
    )

    @Before
    fun setUp() {
        video = folder.newFile("Episode 1 (0a1b2c3d).ts").apply { writeBytes(bytes) }
        secret = folder.newFile("secret.txt").apply { writeText("not to be served") }
        // Files are opened by the URI the route was given, as the content resolver would
        val opener = LocalFileOpener { uri ->
            synchronized(opened) { opened += uri }
            File(uri).takeIf { it.isFile }?.let(::FileInputStream)
        }
        proxy = CastProxyServer(ContextWrapper(null), OkHttpClient(), opener)
        assertTrue(proxy.startSafely())
    }

    @After
    fun tearDown() {
        proxy.stop()
    }

    private fun media(key: String = "episode 1", videoPath: String = video.path) = LocalMedia(
        key = key,
        files = mapOf(
            "video.ts" to LocalFile(videoPath, "video/mp2t"),
            "sub0.vtt" to LocalFile(folder.newFile().apply { writeText("WEBVTT\n") }.path, "text/vtt"),
        ),
        playlists = mapOf("video.m3u8" to LocalPlaylist.Media("video.ts", segments)),
    )

    private fun call(path: String, method: String = "GET", range: String? = null, host: String? = null): okhttp3.Response {
        val request = Request.Builder().url("http://127.0.0.1:${proxy.listeningPort}$path").method(method, null)
        range?.let { request.header("Range", it) }
        host?.let { request.header("Host", it) }
        return client.newCall(request.build()).execute()
    }

    @Test
    fun aFileIsServedWhole() {
        val prefix = proxy.serveLocal(media())
        call("${prefix}video.ts").use { response ->
            assertEquals(200, response.code)
            assertArrayEquals(bytes, response.body.bytes())
            assertEquals("video/mp2t", response.header("Content-Type"))
            assertEquals("2500", response.header("Content-Length"))
            assertEquals("bytes", response.header("Accept-Ranges"))
            assertEquals("*", response.header("Access-Control-Allow-Origin"))
        }
    }

    @Test
    fun aRangeIsServedAsPartialContent() {
        val prefix = proxy.serveLocal(media())
        call("${prefix}video.ts", range = "bytes=1000-2499").use { response ->
            assertEquals(206, response.code)
            assertArrayEquals(bytes.copyOfRange(1_000, 2_500), response.body.bytes())
            assertEquals("bytes 1000-2499/2500", response.header("Content-Range"))
            assertEquals("1500", response.header("Content-Length"))
        }
        call("${prefix}video.ts", range = "bytes=-10").use { response ->
            assertEquals(206, response.code)
            assertArrayEquals(bytes.copyOfRange(2_490, 2_500), response.body.bytes())
        }
        call("${prefix}video.ts", range = "bytes=2500-").use { response ->
            assertEquals(416, response.code)
            assertEquals("bytes */2500", response.header("Content-Range"))
        }
    }

    @Test
    fun aHeadHasTheLengthAndNoBody() {
        val prefix = proxy.serveLocal(media())
        call("${prefix}video.ts", method = "HEAD").use { response ->
            assertEquals(200, response.code)
            assertEquals("2500", response.header("Content-Length"))
            assertEquals("video/mp2t", response.header("Content-Type"))
        }
        call("${prefix}video.ts", method = "HEAD", range = "bytes=0-99").use { response ->
            assertEquals(206, response.code)
            assertEquals("100", response.header("Content-Length"))
        }
    }

    @Test
    fun thePlaylistIsGeneratedOverTheFile() {
        val prefix = proxy.serveLocal(media())
        call("${prefix}video.m3u8").use { response ->
            assertEquals(200, response.code)
            assertEquals("application/vnd.apple.mpegurl", response.header("Content-Type"))
            val body = response.body.string()
            // Relative to the playlist's own URL, with the token
            assertTrue(body, body.contains("#EXT-X-BYTERANGE:1500@1000\n${prefix.removePrefix("/")}video.ts\n"))
        }
    }

    @Test
    fun theReceiverWrappingOurOwnUrlIsServedHere() {
        val prefix = proxy.serveLocal(media())
        val own = "127.0.0.1:${proxy.listeningPort}"
        val wrapped = "/proxy?url=" + URLEncoder.encode("http://$own${prefix}video.ts", "UTF-8") + "&type=video%2Fmp2t"
        call(wrapped, range = "bytes=0-9", host = own).use { response ->
            assertEquals(206, response.code)
            assertArrayEquals(bytes.copyOfRange(0, 10), response.body.bytes())
        }
    }

    @Test
    fun aWrongOrMissingTokenIsRefused() {
        val prefix = proxy.serveLocal(media())
        val token = prefix.removePrefix(LocalMediaRoute.PATH_PREFIX).removeSuffix("-")
        assertEquals(32, token.length)
        val wrong = token.substring(0, 31) + (if (token.last() == '0') "1" else "0")
        call("${LocalMediaRoute.PATH_PREFIX}$wrong-video.ts").use { assertEquals(403, it.code) }
        call("${LocalMediaRoute.PATH_PREFIX}video.ts").use { assertEquals(403, it.code) }
        call("${LocalMediaRoute.PATH_PREFIX}-video.ts").use { assertEquals(403, it.code) }
        assertTrue("no file opened", opened.isEmpty())
    }

    @Test
    fun onlyTheNamesOfTheEpisodeAreServed() {
        val prefix = proxy.serveLocal(media())
        // No path from the URL reaches the storage: a name not served, a path, the file's own name
        listOf("secret.txt", "../secret.txt", "..%2Fsecret.txt", secret.path, video.name, "").forEach { name ->
            call("$prefix$name").use { assertEquals(name, 404, it.code) }
        }
        assertTrue("no file opened", opened.isEmpty())
    }

    @Test
    fun aMissingFileIsNotFound() {
        val prefix = proxy.serveLocal(media(videoPath = File(folder.root, "deleted.ts").path))
        call("${prefix}video.ts").use { assertEquals(404, it.code) }
    }

    @Test
    fun anotherEpisodeGetsAnotherToken() {
        val first = proxy.serveLocal(media())
        assertEquals("the same episode again", first, proxy.serveLocal(media()))
        val second = proxy.serveLocal(media(key = "episode 2"))
        assertNotEquals(first, second)
        call("${first}video.ts").use { assertEquals(403, it.code) }
        call("${second}video.ts").use { assertEquals(200, it.code) }
    }

    @Test
    fun nothingIsServedOnceTheProxyStops() {
        val prefix = proxy.serveLocal(media())
        proxy.stop()
        assertTrue(proxy.startSafely())
        call("${prefix}video.ts").use { assertEquals(403, it.code) }
    }
}
