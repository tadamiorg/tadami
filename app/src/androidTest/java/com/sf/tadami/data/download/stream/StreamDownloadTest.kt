package com.sf.tadami.data.download.stream

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.hippo.unifile.UniFile
import com.sf.tadami.data.download.DownloadNames
import com.sf.tadami.data.download.FileProgress
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.model.Track
import kotlinx.coroutines.runBlocking
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * The stream downloads against a local server, on a real Android runtime: Media3's playlist
 * parser, AES-128, the writes through the content resolver, the resumes.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class StreamDownloadTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val client = OkHttpClient()
    private val http = MediaHttp(client, firstRetryDelayMs = 10)
    private lateinit var server: MockWebServer
    private lateinit var root: File
    private lateinit var dir: UniFile
    private val paths = CopyOnWriteArrayList<String>()
    private val ranges = CopyOnWriteArrayList<String>()

    private val key = ByteArray(16) { (it * 7).toByte() }

    // TS packets: a sync byte every 188 bytes, each segment its own content. 1.1 MB in all.
    private val segments = List(SEGMENT_COUNT) { index ->
        ByteArray(188 * 1000) { i -> if (i % 188 == 0) 0x47 else (index * 31 + i).toByte() }
    }
    private val video = byteArrayOf(0, 0, 0, 0x20) + "ftypisom".encodeToByteArray() +
        ByteArray(300_000) { (it % 251).toByte() }

    @Before
    fun setUp() {
        root = File(context.cacheDir, "download-test").apply {
            deleteRecursively()
            mkdirs()
        }
        dir = UniFile.fromFile(root)!!
        server = MockWebServer()
        server.dispatcher = FakeHost()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
        root.deleteRecursively()
    }

    @Test
    fun aMultivariantPlaylistIsJoinedAndDecrypted() = runBlocking {
        val saved = mutableListOf<FileProgress>()
        val downloaded = HlsDownload(context, http).download(
            url = server.url("/master.m3u8").toString(),
            playlist = null,
            headers = null,
            dir = dir,
            fileBase = "Ep 1",
            quality = "720p",
            audio = false,
            resume = null,
            onAudioRendition = { fail("The variants have their audio muxed") },
            onFileProgress = { saved += it },
            onProgress = {},
        )

        val file = downloaded.file
        assertEquals("Ep 1.ts", file.name)
        assertArrayEquals(segments.reduce(ByteArray::plus), File(root, "Ep 1.ts").readBytes())
        val sizes = segments.map { it.size.toLong() }
        assertEquals(
            FileProgress("Ep 1.ts", hls = true, bytes = file.length(), segments = SEGMENT_COUNT, total = SEGMENT_COUNT.toLong(), sizes = sizes),
            saved.last(),
        )
        // Where each segment is in the joined file, for the cast proxy
        assertEquals(sizes.runningFold(0L, Long::plus).dropLast(1), downloaded.segments!!.map { it.offset })
        assertEquals(sizes, downloaded.segments!!.map { it.length })
        // The 720p variant, not the 360p one
        assertTrue(paths.none { it.startsWith("/low/") })
        // The key once
        assertEquals(1, paths.count { it == "/key" })
    }

    @Test
    fun anHlsDownloadResumesAfterItsWrittenSegments() = runBlocking {
        val written = segments.take(3).reduce(ByteArray::plus)
        // The 4th segment was half written when it stopped
        File(root, "Ep 1.ts").writeBytes(written + segments[3].copyOf(500))
        val resume = FileProgress("Ep 1.ts", hls = true, bytes = written.size.toLong(), segments = 3, total = SEGMENT_COUNT.toLong())

        HlsDownload(context, http).download(
            server.url("/high/media.m3u8").toString(), null, null, dir, "Ep 1", "", false, resume, {}, {}, {},
        )

        assertArrayEquals(segments.reduce(ByteArray::plus), File(root, "Ep 1.ts").readBytes())
        assertEquals(listOf("/high/seg3.ts", "/high/seg4.ts", "/high/seg5.ts"), paths.filter { it.startsWith("/high/seg") }.sorted())
    }

    @Test
    fun aProgressiveDownloadResumesWithARange() = runBlocking {
        File(root, "Ep 2.mp4").writeBytes(video.copyOf(100_000))
        val resume = FileProgress("Ep 2.mp4", hls = false, total = video.size.toLong())

        ProgressiveDownload(context, http).download(
            server.url("/video").toString(), null, dir, "Ep 2", resume, null, {}, {},
        )

        assertArrayEquals(video, File(root, "Ep 2.mp4").readBytes())
        assertEquals(listOf("bytes=100000-"), ranges)
    }

    @Test
    fun aPageIsNotAVideo() = runBlocking {
        try {
            ProgressiveDownload(context, http).download(server.url("/page").toString(), null, dir, "Ep 3", null, null, {}, {})
            fail("A page must fail the stream")
        } catch (e: StreamFailedException) {
            // Expected
        }
    }

    @Test
    fun aStreamIsToldFromItsResponseWithItsSubtitles() = runBlocking {
        val stream = StreamSource(
            // No .m3u8 in the URL: the response tells
            url = server.url("/play?id=1").toString(),
            fullName = "Host 720p",
            quality = "720p",
            subtitleTracks = listOf(Track.SubtitleTrack(server.url("/sub").toString(), "English")),
        )

        val video = StreamDownloader(context, client).download(stream, dir, "Ep 4", emptyMap(), { _, _ -> }, {}).video.file

        assertEquals("Ep 4.ts", video.name)
        val files = DownloadNames.parseFiles("Ep 4", root.list()!!.toList())
        assertEquals("Ep 4.ts", files.video)
        assertEquals(listOf(DownloadNames.TrackFile("Ep 4.English.vtt", "English", "vtt")), files.subtitles)
    }

    private inner class FakeHost : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val path = request.url.encodedPath
            paths += path
            return when {
                path == "/master.m3u8" || path == "/play" -> MockResponse.Builder()
                    .setHeader("Content-Type", "application/vnd.apple.mpegurl")
                    .body(
                        """
                        #EXTM3U
                        #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360
                        low/media.m3u8
                        #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720
                        high/media.m3u8
                        """.trimIndent(),
                    )
                    .build()
                path == "/high/media.m3u8" -> MockResponse.Builder().body(mediaPlaylist()).build()
                path.startsWith("/high/seg") -> {
                    val index = path.removePrefix("/high/seg").removeSuffix(".ts").toInt()
                    MockResponse.Builder().body(Buffer().write(encrypt(segments[index], MEDIA_SEQUENCE + index))).build()
                }
                path == "/key" -> MockResponse.Builder().body(Buffer().write(key)).build()
                path == "/video" -> videoResponse(request.headers["Range"])
                path == "/page" -> MockResponse.Builder()
                    .setHeader("Content-Type", "text/html")
                    .body("<!DOCTYPE html><html><body>File not found</body></html>")
                    .build()
                path == "/sub" -> MockResponse.Builder().body("WEBVTT\n\n00:01.000 --> 00:02.000\nHello\n").build()
                else -> MockResponse.Builder().code(404).build()
            }
        }

        private fun mediaPlaylist() = buildString {
            appendLine("#EXTM3U")
            appendLine("#EXT-X-VERSION:3")
            appendLine("#EXT-X-TARGETDURATION:10")
            appendLine("#EXT-X-MEDIA-SEQUENCE:$MEDIA_SEQUENCE")
            appendLine("#EXT-X-KEY:METHOD=AES-128,URI=\"/key\"")
            segments.indices.forEach {
                appendLine("#EXTINF:10.0,")
                appendLine("seg$it.ts")
            }
            appendLine("#EXT-X-ENDLIST")
        }

        private fun videoResponse(range: String?): MockResponse {
            if (range == null) {
                return MockResponse.Builder().setHeader("Content-Type", "video/mp4").body(Buffer().write(video)).build()
            }
            ranges += range
            val start = range.removePrefix("bytes=").removeSuffix("-").toInt()
            return MockResponse.Builder()
                .code(206)
                .setHeader("Content-Type", "video/mp4")
                .setHeader("Content-Range", "bytes $start-${video.size - 1}/${video.size}")
                .body(Buffer().write(video.copyOfRange(start, video.size)))
                .build()
        }

        private fun encrypt(data: ByteArray, sequence: Int): ByteArray {
            return Cipher.getInstance("AES/CBC/PKCS5Padding").run {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(HlsCrypto.iv(null, sequence.toLong())))
                doFinal(data)
            }
        }
    }

    private companion object {
        const val SEGMENT_COUNT = 6
        const val MEDIA_SEQUENCE = 5
    }
}
