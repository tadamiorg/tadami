package com.sf.tadami.ui.animeinfos.episode.cast.preview

import android.os.SystemClock
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.sf.tadami.network.player.PlayerNetworkHelper
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewPrefetch
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * The background prefetch of the cast seekbar preview, on a real Android runtime against public
 * test streams: every step pushed to the "receiver", nearest the playback position first, re-centred
 * after a seek, scrubs served first, the time and data a whole episode costs (logged:
 * `adb logcat -s CastPreviewPrefetchTest CastSeekPreview`). Needs the network, so it only runs when
 * asked: `-e castPreviewStreams true`.
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
                playerNetworkHelper = Injekt.get<PlayerNetworkHelper>(),
                send = { messages.put(Json.parseToJsonElement(it).jsonObject) },
                localFileUri = { url -> if (url == LOCAL_TS_URL) localTsFile.toURI().toString() else null },
                receiverCapable = { true },
                isOnWifi = { true },
                // The TV plays steadily at positionMs
                playback = { CastSeekPreview.CastPlayback(PreviewPrefetch.Playback.PLAYING, positionMs) },
            )
            preview.settings = CastSeekPreview.Settings(enabled = true, onlyOnWifi = false, userAgent = "Tadami test")
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
    private val JsonObject.pushed get() = this["requestId"] == null

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

    private fun logStats(name: String, stats: CastSeekPreview.Stats, elapsedMs: Long) {
        Log.i(
            TAG,
            "$name: ${stats.frames} frames + ${stats.withoutFrame} without of ${stats.steps} steps, prefetch " +
                "${(stats.prefetchMs ?: 0) / 1000} s (${elapsedMs / 1000} s from the announcement), " +
                String.format(Locale.ROOT, "%.1f MB downloaded", stats.downloadedBytes / 1_000_000.0),
        )
    }

    @Test
    fun hlsEpisodeIsPrefetchedWhole() {
        // Mux's Big Buck Bunny HLS test stream, 10:34: 101 steps of 6.3 s; the TV at 5:00
        positionMs = 300_000
        val started = SystemClock.elapsedRealtime()
        instrumentation.runOnMainSync { preview.onSource(source(HLS_ID, HLS, HLS_DURATION_MS)) }
        val pushes = awaitComplete(15 * 60_000L)
        val stats = stats()
        logStats("hls", stats, SystemClock.elapsedRealtime() - started)

        assertEquals(101, stats.steps)
        assertTrue(pushes.all { it.pushed })
        assertEquals("every step pushed once", (0 until stats.steps).toList(), pushes.map { it.step }.sorted())
        assertEquals(stats.frames + stats.withoutFrame, stats.steps)
        assertTrue("mostly frames: $stats", stats.frames >= stats.steps * 9 / 10)
        // Nearest the TV's position first
        val center = previewBucketOf(300_000, HLS_DURATION_MS)
        assertEquals(center, pushes.first().step)
        assertTrue(pushes.take(5).all { kotlin.math.abs(it.step - center) <= 3 })
        Log.i(TAG, "hls push order: ${pushes.map { it.step }}")

        // The receiver restarted, holding nothing: everything goes again, paced, nothing decoded
        val repushStart = SystemClock.elapsedRealtime()
        instrumentation.runOnMainSync { preview.onSource(source(HLS_ID, HLS, HLS_DURATION_MS)) }
        val again = mutableListOf<JsonObject>()
        while (again.size < stats.steps) again += messages.poll(5, TimeUnit.SECONDS) ?: break
        Log.i(TAG, "hls re-push: ${again.size} frames in ${SystemClock.elapsedRealtime() - repushStart} ms")
        assertEquals((0 until stats.steps).toList(), again.map { it.step }.sorted())
        assertEquals(stats.downloadedBytes, stats().downloadedBytes)
    }

    @Test
    fun recentredAfterSeekAndScrubsFirst() {
        positionMs = 0
        instrumentation.runOnMainSync { preview.onSource(source(HLS_ID, HLS, HLS_DURATION_MS)) }
        // A few pushes around the start
        val first = (1..3).map { messages.poll(60, TimeUnit.SECONDS) ?: throw AssertionError("no push") }
        assertTrue(first.all { it.pushed && it.step <= 3 })

        // The TV seeks to 9:00: the prefetch moves there
        positionMs = 540_000
        val target = previewBucketOf(540_000, HLS_DURATION_MS)
        val next = (1..4).map { messages.poll(60, TimeUnit.SECONDS) ?: throw AssertionError("no push") }
        Log.i(TAG, "after the seek to step $target: ${next.map { it.step }}")
        assertTrue(next.any { kotlin.math.abs(it.step - target) <= 2 })

        // A scrub far from both goes first, answered with its request id
        val scrub = CastPreviewRequest(
            type = CastPreviewProtocol.TYPE_REQUEST,
            requestId = 77,
            sourceId = HLS_ID,
            url = HLS,
            step = 40,
            duration = HLS_DURATION_MS,
        )
        val asked = SystemClock.elapsedRealtime()
        instrumentation.runOnMainSync { preview.onRequest(scrub) }
        var answer: JsonObject? = null
        while (answer == null) {
            val message = messages.poll(20, TimeUnit.SECONDS) ?: throw AssertionError("no answer to the scrub")
            if (!message.pushed) answer = message
        }
        Log.i(TAG, "scrub of step 40 answered in ${SystemClock.elapsedRealtime() - asked} ms during the prefetch")
        assertEquals(40, answer.step)
        assertEquals("77", answer["requestId"]!!.jsonPrimitive.content)
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
        logStats("local-ts", stats, SystemClock.elapsedRealtime() - started)
        assertEquals(12, stats.steps)
        assertEquals((0 until 12).toList(), pushes.map { it.step }.sorted())
        assertEquals("a download costs no data", 0L, stats.downloadedBytes)
    }

    private companion object {
        const val TAG = "CastPreviewPrefetchTest"
        const val HLS = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        const val HLS_ID = "1|https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8|mux|test"
        const val HLS_DURATION_MS = 634_584L
        const val LOCAL_TS_URL = "http://192.168.1.4:8214/local-00000000000000000000000000000000-video.m3u8"
    }
}
