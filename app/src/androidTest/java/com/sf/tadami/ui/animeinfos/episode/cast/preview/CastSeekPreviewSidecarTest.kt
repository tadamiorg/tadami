package com.sf.tadami.ui.animeinfos.episode.cast.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.hippo.unifile.UniFile
import com.sf.tadami.data.download.DownloadNames
import com.sf.tadami.data.download.EpisodePreviews
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewPrefetch
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewSidecar
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketOf
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketSizeMs
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * The cast seekbar preview of a downloaded episode whose frames are kept next to it, on a real Android runtime,
 * with no receiver: every frame pushed as soon as the stream is announced, nearest the TV's position first, over
 * a channel confirming each push after a set delay, and the time the ~100 pushes take
 * (`adb logcat -s CastPreviewSidecarTest CastSeekPreview`); a streamed episode makes nothing.
 */
@UnstableApi
@LargeTest
@RunWith(AndroidJUnit4::class)
class CastSeekPreviewSidecarTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    // Parsed by the test thread: the channel's thread only notes when each came
    private val sent = LinkedBlockingQueue<Pair<Long, String>>()
    private val scope = MainScope()
    private val main = Handler(Looper.getMainLooper())
    private lateinit var dir: File
    private lateinit var previews: EpisodePreviews
    private var grabbers = 0

    @Volatile
    private var sentAfterMs = 0L

    @Before
    fun setUp() {
        dir = File(context.cacheDir, "cast-sidecar-test/$EPISODE").apply {
            deleteRecursively()
            mkdirs()
        }
        // The video is never read: its frames are all in the sidecar
        val video = File(dir, "$EPISODE.ts").apply { writeBytes(ByteArray(VIDEO_BYTES)) }
        previews = EpisodePreviews(UniFile.fromFile(dir)!!, EPISODE, UniFile.fromFile(video)!!)
        assertTrue(previews.write(context, sidecar()))
    }

    @After
    fun tearDown() {
        scope.cancel()
        dir.parentFile?.deleteRecursively()
    }

    /** 100 frames of 256×144, step 29 without one, encoded as the cast's. */
    private fun sidecar(): PreviewSidecar {
        val entries = (0..99).map { step ->
            if (step == 29) return@map PreviewSidecar.Entry(step, -1, null)
            val bitmap = Bitmap.createBitmap(256, 144, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.rgb(step * 2, 120, 255 - step * 2))
            val paint = Paint().apply { textSize = 48f; color = Color.WHITE }
            // Some detail, as a real frame has
            repeat(40) { i -> canvas.drawText("$step·$i", (i * 37 % 256).toFloat(), (i * 23 % 144).toFloat(), paint) }
            val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
            bitmap.recycle()
            PreviewSidecar.Entry(step, step * 14_300L + 500, jpeg)
        }
        return PreviewSidecar(VIDEO_BYTES.toLong(), DURATION_MS, previewBucketSizeMs(DURATION_MS), 256, 144, entries)
    }

    private fun newPreview(positionMs: Long): CastSeekPreview {
        var preview: CastSeekPreview? = null
        instrumentation.runOnMainSync {
            preview = CastSeekPreview(
                context = context,
                send = { sent.put(SystemClock.elapsedRealtime() to it) },
                localFileUri = { url -> if (url == LOCAL_URL) UniFile.fromFile(File(dir, "$EPISODE.ts"))!!.uri.toString() else null },
                receiverCapable = { true },
                playback = { CastSeekPreview.CastPlayback(PreviewPrefetch.Playback.PLAYING, positionMs) },
                scope = scope,
                localPreviews = { url -> if (url == LOCAL_URL) previews else null },
                sendPush = { message, onSent ->
                    sent.put(SystemClock.elapsedRealtime() to message)
                    // The channel takes it after a while
                    main.postDelayed(onSent, sentAfterMs)
                },
                newGrabber = {
                    grabbers++
                    CastFrameGrabber(context)
                },
            ).apply { settings = CastSeekPreview.Settings(enabled = true) }
        }
        return preview!!
    }

    private fun source(url: String) = CastPreviewSource(
        type = CastPreviewProtocol.TYPE_SOURCE,
        sourceId = "7|$url|Downloaded|",
        url = url,
        duration = DURATION_MS,
    )

    /** The next message within [seconds], parsed, with when it was sent. */
    private fun poll(seconds: Long): Pair<Long, JsonObject>? =
        sent.poll(seconds, TimeUnit.SECONDS)?.let { (at, message) -> at to Json.parseToJsonElement(message).jsonObject }

    private val JsonObject.step get() = this["step"]!!.jsonPrimitive.content.toInt()
    private val JsonObject.status get() = this["status"]!!.jsonPrimitive.content

    /** Announces the downloaded episode at [positionMs], the channel taking each push after [sentAfterMs]: the pushes. */
    private fun pushAll(positionMs: Long, sentAfterMs: Long): List<Pair<Long, JsonObject>> {
        this.sentAfterMs = sentAfterMs
        val preview = newPreview(positionMs)
        val steps = previewBucketOf(DURATION_MS - 1, DURATION_MS) + 1
        val announcedAt = SystemClock.elapsedRealtime()
        instrumentation.runOnMainSync { preview.onSource(source(LOCAL_URL)) }
        val pushes = (1..steps).map { poll(30) ?: throw AssertionError("${it - 1} pushes only") }
        val firstMs = pushes.first().first - announcedAt
        val lastMs = pushes.last().first - announcedAt
        Log.i(TAG, "Channel taking each push in $sentAfterMs ms: ${pushes.size} pushes, the first $firstMs ms, the last $lastMs ms after the announcement")
        assertNull("nothing more", poll(1))
        instrumentation.runOnMainSync { preview.destroy() }
        return pushes.map { (time, message) -> time - announcedAt to message }
    }

    @Test
    fun everyFrameIsPushedAtOnceNearestThePositionFirst() {
        val pushes = pushAll(positionMs = 600_000, sentAfterMs = 0).map { it.second }
        assertEquals("steps 0..100 once", (0..100).toList(), pushes.map { it.step }.sorted())
        assertTrue("pushed, no request id", pushes.all { it["requestId"] == null })
        assertEquals("none", pushes.single { it.step == 29 }.status)
        assertTrue(pushes.filter { it.step != 29 }.all { it.status == "ok" && it["jpeg"] != null })
        val center = previewBucketOf(600_000, DURATION_MS)
        assertEquals(center, pushes.first().step)
        val distances = pushes.map { abs(it.step - center) }
        assertEquals("nearest first", distances.sorted(), distances)
        assertEquals("nothing decoded", 0, grabbers)
    }

    @Test
    fun pushTimeWithASlowerChannel() {
        for (sentAfterMs in listOf(0L, 10L, 30L)) {
            val pushes = pushAll(positionMs = 0, sentAfterMs = sentAfterMs)
            assertEquals(101, pushes.size)
        }
        assertEquals(0, grabbers)
    }

    @Test
    fun aScrubIsAnsweredFromTheSidecarAtOnce() {
        val preview = newPreview(positionMs = 0)
        instrumentation.runOnMainSync {
            preview.settings = CastSeekPreview.Settings(enabled = true, prefetch = false)
            preview.onSource(source(LOCAL_URL))
        }
        // No prefetch: nothing pushed, the scrub asks
        assertNull(poll(1))
        val askedAt = SystemClock.elapsedRealtime()
        instrumentation.runOnMainSync { preview.onRequest(source(LOCAL_URL).requestFor(64).copy(requestId = 3)) }
        val (at, reply) = poll(5)!!
        Log.i(TAG, "Scrub of a step kept next to the episode answered in ${at - askedAt} ms")
        assertEquals(64, reply.step)
        assertEquals("3", reply["requestId"]!!.jsonPrimitive.content)
        assertEquals("ok", reply.status)
        assertTrue(at - askedAt < 300)
        instrumentation.runOnMainSync { preview.destroy() }
        assertEquals(0, grabbers)
    }

    @Test
    fun aStreamedEpisodeMakesNothing() {
        val preview = newPreview(positionMs = 0)
        val stream = "https://h.example/ep.m3u8"
        instrumentation.runOnMainSync { preview.onSource(source(stream)) }
        assertNull("nothing pushed", poll(2))
        var stats: CastSeekPreview.Stats? = null
        instrumentation.runOnMainSync { stats = preview.stats() }
        assertNull("not even prefetched", stats!!.sourceId)
        // A scrub's request (an older phone announced it) is refused at once
        instrumentation.runOnMainSync { preview.onRequest(source(stream).requestFor(3).copy(requestId = 1)) }
        val reply = poll(1)?.second
        assertNotNull(reply)
        assertEquals("off", reply!!.status)
        assertNull(poll(1))
        instrumentation.runOnMainSync { preview.destroy() }
        assertEquals("never decoded", 0, grabbers)
        assertFalse(File(dir, DownloadNames.previewsTmpFileName(EPISODE)).exists())
    }

    private companion object {
        const val TAG = "CastPreviewSidecarTest"
        const val EPISODE = "Episode 3 (0a1b2c3d)"
        const val VIDEO_BYTES = 4096

        // 23:50.026: 14.3 s steps, 0..100 on the receiver
        const val DURATION_MS = 1_430_026L
        const val LOCAL_URL = "http://192.168.1.4:8214/local-00000000000000000000000000000000-video.m3u8"
    }
}
