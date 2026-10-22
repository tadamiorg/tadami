package com.sf.tadami.ui.animeinfos.episode.cast.preview

import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.sf.tadami.network.player.PlayerNetworkHelper
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Base64
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * The cast seekbar preview generator on a real Android runtime, against public test streams: an HLS
 * stream, a progressive MP4, and downloads (the local-file path): an MP4 and HLS segments joined in
 * one MPEG-TS file. Needs the network, so it only runs when asked: `-e castPreviewStreams true`. Logs each frame's decode time and JPEG size
 * (`adb logcat -s CastPreviewStreamsTest CastSeekPreview CastFrameGrabber`) and keeps the frames in
 * the app's external files (`cast-preview/`) to look at.
 */
@UnstableApi
@LargeTest
@RunWith(AndroidJUnit4::class)
class CastSeekPreviewStreamsTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val replies = LinkedBlockingQueue<String>()
    private lateinit var preview: CastSeekPreview
    private lateinit var localFile: File
    private lateinit var localTsFile: File
    private var nextRequestId = 1L
    private val outDir by lazy { File(context.getExternalFilesDir(null), "cast-preview").apply { mkdirs() } }

    @Before
    fun setUp() {
        assumeTrue(
            "network test streams: run with -e castPreviewStreams true",
            InstrumentationRegistry.getArguments().getString("castPreviewStreams") == "true",
        )
        localFile = File(context.cacheDir, "cast-preview-local.mp4")
        localTsFile = File(context.cacheDir, "cast-preview-local.ts")
        instrumentation.runOnMainSync {
            preview = CastSeekPreview(
                context = context,
                playerNetworkHelper = Injekt.get<PlayerNetworkHelper>(),
                send = { replies.put(it) },
                // The proxy URLs of a "downloaded episode" stand for the local files
                localFileUri = { url ->
                    when (url) {
                        LOCAL_URL -> localFile.toURI().toString()
                        LOCAL_TS_URL -> localTsFile.toURI().toString()
                        else -> null
                    }
                },
                receiverCapable = { true },
                isOnWifi = { true },
            )
            preview.settings = CastSeekPreview.Settings(enabled = true, onlyOnWifi = false, userAgent = USER_AGENT)
        }
    }

    @After
    fun tearDown() {
        if (::preview.isInitialized) instrumentation.runOnMainSync { preview.destroy() }
    }

    private class Reply(val status: String, val step: Int, val sourceId: String, val jpeg: ByteArray?, val chars: Int, val ms: Long)

    private fun ask(sourceId: String, url: String, durationMs: Long, step: Int, headers: Map<String, String>? = null): Reply {
        val request = CastPreviewRequest(
            type = CastPreviewProtocol.TYPE_REQUEST,
            requestId = nextRequestId++,
            sourceId = sourceId,
            url = url,
            headers = headers,
            step = step,
            duration = durationMs,
        )
        val started = SystemClock.elapsedRealtime()
        instrumentation.runOnMainSync { preview.onRequest(request) }
        val message = replies.poll(20, TimeUnit.SECONDS) ?: throw AssertionError("no reply for step $step of $sourceId")
        val elapsed = SystemClock.elapsedRealtime() - started
        val reply = Json.parseToJsonElement(message).jsonObject
        val jpeg = reply["jpeg"]?.jsonPrimitive?.content?.let { Base64.getDecoder().decode(it) }
        return Reply(
            status = reply["status"]!!.jsonPrimitive.content,
            step = reply["step"]!!.jsonPrimitive.content.toInt(),
            sourceId = reply["sourceId"]!!.jsonPrimitive.content,
            jpeg = jpeg,
            chars = message.length,
            ms = elapsed,
        ).also {
            Log.i(TAG, "$sourceId step $step: ${it.status} in ${it.ms} ms, JPEG ${jpeg?.size ?: 0} B, message ${it.chars} chars")
        }
    }

    private fun checkFrame(name: String, reply: Reply) {
        assertEquals("$name step ${reply.step}", CastPreviewProtocol.STATUS_OK, reply.status)
        val jpeg = reply.jpeg!!
        File(outDir, "$name-step${reply.step}.jpg").writeBytes(jpeg)
        assertTrue("$name within the base64 budget", CastPreviewProtocol.base64Length(jpeg.size) <= CastPreviewProtocol.MAX_FRAME_BASE64_CHARS)
        assertTrue("$name within a cast message", reply.chars <= CastPreviewProtocol.MAX_MESSAGE_CHARS)
        val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        assertEquals(256, bitmap.width)
        // A picture, not a blank surface: its pixels differ
        val pixels = IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
        assertTrue("$name step ${reply.step} isn't uniform", pixels.distinct().size > 50)
    }

    private fun exercise(name: String, url: String, durationMs: Long, steps: List<Int>, headers: Map<String, String>? = null) {
        val sourceId = "1|$url|$name|test"
        val first = steps.map { step -> ask(sourceId, url, durationMs, step, headers).also { checkFrame(name, it) } }
        // Asked again: from the cache, the same frame, no decode
        val again = ask(sourceId, url, durationMs, steps[1], headers)
        assertArrayEquals(first[1].jpeg, again.jpeg)
        assertTrue("$name cached reply took ${again.ms} ms", again.ms < 200)
        Log.i(
            TAG,
            "$name: decodes ${first.map { it.ms }} ms, JPEGs ${first.map { it.jpeg!!.size }} B, cached reply ${again.ms} ms",
        )
    }

    @Test
    fun hlsStream() {
        // Mux's Big Buck Bunny HLS test stream: 10:34, several renditions (the lowest is decoded)
        exercise("hls", HLS, HLS_DURATION_MS, listOf(3, 20, 50, 99))
    }

    @Test
    fun progressiveMp4() {
        // Internet Archive's Big Buck Bunny, 720p H.264 (redirected to a storage node)
        exercise(
            "mp4",
            "https://archive.org/download/BigBuckBunny_124/Content/big_buck_bunny_720p_surround.mp4",
            596_504,
            // Not 50: the film is black there (299 to 302 s)
            listOf(3, 20, 60, 97),
            headers = mapOf("User-Agent" to USER_AGENT, "Referer" to "https://tadami.example/"),
        )
    }

    @Test
    fun downloadedFile() {
        OkHttpClient().newCall(Request.Builder().url(SMALL_MP4).build()).execute().use { response ->
            assertTrue(response.isSuccessful)
            localFile.writeBytes(response.body.bytes())
        }
        // 10 s: 5 s steps
        exercise("local", LOCAL_URL, 10_000, listOf(0, 1))
    }

    @Test
    fun downloadedJoinedTs() {
        // What a downloaded HLS episode is: its segments joined in one MPEG-TS file (the first minute here)
        val client = OkHttpClient()
        val playlistUrl = "https://test-streams.mux.dev/x36xhzz/url_2/193039199_mp4_h264_aac_ld_7.m3u8"
        val playlist = client.newCall(Request.Builder().url(playlistUrl).build()).execute().use { it.body.string() }
        val segments = playlist.lines().filter { it.isNotBlank() && !it.startsWith("#") }.take(6)
        localTsFile.outputStream().use { out ->
            segments.forEach { segment ->
                val url = playlistUrl.substringBeforeLast('/') + "/" + segment
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    assertTrue(response.isSuccessful)
                    response.body.byteStream().copyTo(out)
                }
            }
        }
        // 60 s: 5 s steps (not the first: the film fades in from black)
        exercise("local-ts", LOCAL_TS_URL, 60_000, listOf(2, 5, 8, 10))
    }

    @Test
    fun anotherStreamDecodesAgainAndSettingOffAnswersOff() {
        val hls = HLS
        val a = ask("1|$hls|a|test", hls, HLS_DURATION_MS, 10)
        checkFrame("switch-a", a)
        val b = ask("2|$hls|b|test", hls, HLS_DURATION_MS, 10)
        checkFrame("switch-b", b)
        // Back to the first stream: its frames were dropped, decoded again
        val again = ask("1|$hls|a|test", hls, HLS_DURATION_MS, 10)
        assertEquals(CastPreviewProtocol.STATUS_OK, again.status)
        Log.i(TAG, "switch: decode ${a.ms} ms, other stream ${b.ms} ms, back ${again.ms} ms")

        instrumentation.runOnMainSync { preview.settings = preview.settings.copy(enabled = false) }
        assertEquals(CastPreviewProtocol.STATUS_OFF, ask("1|$hls|a|test", hls, HLS_DURATION_MS, 11).status)
    }

    private companion object {
        const val TAG = "CastPreviewStreamsTest"
        const val HLS = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        const val HLS_DURATION_MS = 634_584L
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) Tadami cast preview test"
        const val LOCAL_URL = "http://192.168.1.4:8214/local-00000000000000000000000000000000-video.mp4"
        const val LOCAL_TS_URL = "http://192.168.1.4:8214/local-00000000000000000000000000000000-video.m3u8"
        const val SMALL_MP4 = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4"
    }
}
