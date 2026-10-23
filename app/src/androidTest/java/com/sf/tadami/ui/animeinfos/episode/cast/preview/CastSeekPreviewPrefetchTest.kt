package com.sf.tadami.ui.animeinfos.episode.cast.preview

import android.os.SystemClock
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewPrefetch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * The background prefetch of the cast seekbar preview of a downloaded episode, on a real Android runtime: every
 * step pushed to the "receiver" (logged: `adb logcat -s CastPreviewPrefetchTest CastSeekPreview`). Its file is
 * made from a public test stream, so it only runs when asked: `-e castPreviewStreams true`.
 */
@UnstableApi
@LargeTest
@RunWith(AndroidJUnit4::class)
class CastSeekPreviewPrefetchTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val messages = LinkedBlockingQueue<JsonObject>()
    private lateinit var preview: CastSeekPreview
    private lateinit var localTsFile: File

    @Volatile
    private var positionMs = 0L

    @Before
    fun setUp() {
        assumeTrue(
            "network test streams: run with -e castPreviewStreams true",
            InstrumentationRegistry.getArguments().getString("castPreviewStreams") == "true",
        )
        localTsFile = File(context.cacheDir, "cast-preview-prefetch.ts")
        instrumentation.runOnMainSync {
            preview = CastSeekPreview(
                context = context,
                send = { messages.put(Json.parseToJsonElement(it).jsonObject) },
                localFileUri = { url -> if (url == LOCAL_TS_URL) localTsFile.toURI().toString() else null },
                receiverCapable = { true },
                // The TV plays steadily at positionMs
                playback = { CastSeekPreview.CastPlayback(PreviewPrefetch.Playback.PLAYING, positionMs) },
            )
            preview.settings = CastSeekPreview.Settings(enabled = true)
        }
    }

    @After
    fun tearDown() {
        if (::preview.isInitialized) instrumentation.runOnMainSync { preview.destroy() }
    }

    private fun source(sourceId: String, url: String, durationMs: Long, steps: List<Int> = emptyList()) = CastPreviewSource(
        type = CastPreviewProtocol.TYPE_SOURCE,
        sourceId = sourceId,
        url = url,
        duration = durationMs,
        steps = steps,
    )

    private fun stats(): CastSeekPreview.Stats {
        var stats: CastSeekPreview.Stats? = null
        instrumentation.runOnMainSync { stats = preview.stats() }
        return stats!!
    }

    private val JsonObject.step get() = this["step"]!!.jsonPrimitive.content.toInt()

    /** Waits for the prefetch to end, the pushes in the order they came. */
    private fun awaitComplete(timeoutMs: Long): List<JsonObject> {
        val pushes = mutableListOf<JsonObject>()
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            messages.poll(2, TimeUnit.SECONDS)?.let { pushes += it }
            if (stats().complete && messages.isEmpty()) {
                // The last pushes are paced
                while (true) pushes += messages.poll(1, TimeUnit.SECONDS) ?: break
                return pushes
            }
        }
        throw AssertionError("prefetch not complete after $timeoutMs ms: ${stats()}")
    }

    @Test
    fun downloadIsPrefetchedWhole() {
        // A downloaded HLS episode: its segments joined in one MPEG-TS file (the first minute here)
        val client = OkHttpClient()
        val playlistUrl = "https://test-streams.mux.dev/x36xhzz/url_2/193039199_mp4_h264_aac_ld_7.m3u8"
        val playlist = client.newCall(Request.Builder().url(playlistUrl).build()).execute().use { it.body.string() }
        localTsFile.outputStream().use { out ->
            playlist.lines().filter { it.isNotBlank() && !it.startsWith("#") }.take(6).forEach { segment ->
                client.newCall(Request.Builder().url(playlistUrl.substringBeforeLast('/') + "/" + segment).build())
                    .execute().use { it.body.byteStream().copyTo(out) }
            }
        }
        positionMs = 20_000
        val started = SystemClock.elapsedRealtime()
        instrumentation.runOnMainSync { preview.onSource(source("1|local|ts|test", LOCAL_TS_URL, 60_000)) }
        val pushes = awaitComplete(3 * 60_000L)
        val stats = stats()
        Log.i(TAG, "local-ts: $stats, ${SystemClock.elapsedRealtime() - started} ms from the announcement")
        assertEquals(12, stats.steps)
        assertEquals((0 until 12).toList(), pushes.map { it.step }.sorted())
    }

    private companion object {
        const val TAG = "CastPreviewPrefetchTest"
        const val LOCAL_TS_URL = "http://192.168.1.4:8214/local-00000000000000000000000000000000-video.m3u8"
    }
}
