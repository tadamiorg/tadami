package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import android.content.ContextWrapper
import com.hippo.unifile.MemoryUniFile
import com.sf.tadami.data.download.EpisodePreviews
import com.sf.tadami.data.download.SegmentMetadata
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewSidecar
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketSizeMs
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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
    fun theSeekbarPreviewFindsTheFileBehindAServedUrl() {
        val prefix = proxy.serveLocal(media())
        val base = "http://192.168.1.4:${proxy.listeningPort}$prefix"
        // A media playlist leads to its joined file, a file to itself
        assertEquals(video.path, proxy.localFileUri("${base}video.m3u8"))
        assertEquals(video.path, proxy.localFileUri("${base}video.ts"))
        // Nothing else: an unknown name, another token, an online URL
        assertEquals(null, proxy.localFileUri("${base}secret.txt"))
        assertEquals(null, proxy.localFileUri("http://192.168.1.4:8214${LocalMediaRoute.PATH_PREFIX}0123-video.ts"))
        assertEquals(null, proxy.localFileUri("https://host.example/ep.m3u8"))
        assertEquals(null, proxy.localFileUri("not a url"))
        assertTrue("no file opened", opened.isEmpty())
    }

    @Test
    fun theSeekbarPreviewFollowsTheMasterPlaylistToTheVideo() {
        val withMaster = media().let {
            it.copy(playlists = it.playlists + ("master.m3u8" to LocalPlaylist.Master("video.m3u8", emptyList(), 1_000)))
        }
        val prefix = proxy.serveLocal(withMaster)
        assertEquals(video.path, proxy.localFileUri("http://127.0.0.1:${proxy.listeningPort}${prefix}master.m3u8"))
    }

    @Test
    fun theSeekbarPreviewFindsNothingOnceTheProxyStops() {
        val prefix = proxy.serveLocal(media())
        proxy.stop()
        assertEquals(null, proxy.localFileUri("http://127.0.0.1:8214${prefix}video.ts"))
    }

    @Test
    fun nothingIsServedOnceTheProxyStops() {
        val prefix = proxy.serveLocal(media())
        proxy.stop()
        assertTrue(proxy.startSafely())
        call("${prefix}video.ts").use { assertEquals(403, it.code) }
    }

    // The seekbar preview frames of a downloaded episode, kept next to its video, for the TV app

    private val previewDir = MemoryUniFile.root("Episode 1")
    private val previewVideo = previewDir.file("Episode 1.ts", ByteArray(64))
    private val previews = EpisodePreviews(previewDir, "Episode 1", previewVideo)

    private fun sidecarBytes(videoLength: Long = previewVideo.length()): ByteArray {
        val durationMs = 1_430_026L
        return PreviewSidecar.encode(
            PreviewSidecar(
                videoLength = videoLength,
                durationMs = durationMs,
                stepMs = previewBucketSizeMs(durationMs),
                frameWidth = 256,
                frameHeight = 144,
                entries = listOf(
                    PreviewSidecar.Entry(0, 0L, byteArrayOf(1, 2, 3)),
                    PreviewSidecar.Entry(1, 14_300L, null),
                ),
            ),
        )
    }

    private fun keepSidecar(bytes: ByteArray) {
        previewDir.file("Episode 1.preview.tdp", bytes)
    }

    @Test
    fun theTvAppGetsThePreviewFramesOfTheEpisode() {
        val sidecar = sidecarBytes()
        keepSidecar(sidecar)
        val prefix = proxy.serveLocal(media().copy(previews = previews))
        call("${prefix}previews.tdp").use { response ->
            assertEquals(200, response.code)
            assertArrayEquals(sidecar, response.body.bytes())
            assertEquals("application/octet-stream", response.header("Content-Type"))
            assertEquals("${sidecar.size}", response.header("Content-Length"))
        }
        call("${prefix}previews.tdp", method = "HEAD").use { response ->
            assertEquals(200, response.code)
            assertEquals("${sidecar.size}", response.header("Content-Length"))
            assertEquals(0, response.body.bytes().size)
        }
        // Not a file the cast's seekbar preview decodes
        assertNull(proxy.localFileUri("http://127.0.0.1:${proxy.listeningPort}${prefix}previews.tdp"))
        assertTrue("no file opened", opened.isEmpty())
    }

    @Test
    fun noUsablePreviewFramesAreNotFound() {
        // None kept with the episode, none written yet, frames of another video, a broken file
        val noPreviews = proxy.serveLocal(media())
        call("${noPreviews}previews.tdp").use { assertEquals(404, it.code) }
        val prefix = proxy.serveLocal(media().copy(previews = previews))
        call("${prefix}previews.tdp").use { assertEquals(404, it.code) }
        keepSidecar(sidecarBytes(videoLength = previewVideo.length() + 1))
        call("${prefix}previews.tdp").use { assertEquals(404, it.code) }
        call("${prefix}previews.tdp", method = "HEAD").use { assertEquals(404, it.code) }
        keepSidecar(sidecarBytes().copyOf(40))
        call("${prefix}previews.tdp").use { assertEquals(404, it.code) }
    }

    @Test
    fun thePreviewFramesOfAWrongTokenOrOfNoEpisodeAreNotFound() {
        keepSidecar(sidecarBytes())
        val prefix = proxy.serveLocal(media().copy(previews = previews))
        val token = prefix.removePrefix(LocalMediaRoute.PATH_PREFIX).removeSuffix("-")
        val wrong = token.substring(0, 31) + (if (token.last() == '0') "1" else "0")
        call("${LocalMediaRoute.PATH_PREFIX}$wrong-previews.tdp").use { assertEquals(404, it.code) }
        call("${LocalMediaRoute.PATH_PREFIX}previews.tdp").use { assertEquals(404, it.code) }
        call("${LocalMediaRoute.PATH_PREFIX}-previews.tdp").use { assertEquals(404, it.code) }
        // Another episode served since: its own frames only, under its own token
        val other = proxy.serveLocal(media(key = "episode 2"))
        call("${prefix}previews.tdp").use { assertEquals(404, it.code) }
        call("${other}previews.tdp").use { assertEquals(404, it.code) }
        // Served again: a new URL, so the TV app's frames kept by URL are never another episode's; none once the proxy stops
        proxy.serveLocal(media().copy(previews = previews)).let { served ->
            assertNotEquals(prefix, served)
            call("${served}previews.tdp").use { assertEquals(200, it.code) }
            proxy.stop()
            assertTrue(proxy.startSafely())
            call("${served}previews.tdp").use { assertEquals(404, it.code) }
        }
    }

    @Test
    fun noFileOrPlaylistTakesTheNameOfThePreviewFrames() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalMedia(key = "episode", files = mapOf("previews.tdp" to LocalFile(video.path, "video/mp2t")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            LocalMedia(key = "episode", files = emptyMap(), playlists = mapOf("previews.tdp" to LocalPlaylist.Media("video.ts", segments)))
        }
    }
}
