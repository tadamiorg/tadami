package com.sf.tadami.ui.animeinfos.episode.cast.preview

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.sf.tadami.ui.animeinfos.episode.player.preview.PREVIEW_FETCH_TIMEOUT_MS
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewLoadControl
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewRenderersFactory
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewTrackSelectionParameters
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference

/**
 * Decodes preview frames of one stream at a time, with no view: a muted, video-only ExoPlayer (the
 * phone player's preview configuration) renders into an offscreen GL surface ([OffscreenFrameReader])
 * and the frame after each seek is read back, downscaled, as a JPEG within the cast message budget.
 * Works with the phone's player screen closed, from the cast session's service, and in the background
 * for the seekbar preview frames kept next to a downloaded episode (PreviewGenerator).
 *
 * Everything runs on its own thread; results are delivered there. One grab at a time: a new grab or
 * [setSource] drops the grab in flight (its result is never delivered), and a frame is only taken
 * near the time asked, never one the dropped grab's seek left behind; [release] ends the thread.
 */
@UnstableApi
class CastFrameGrabber(private val context: Context) {

    sealed interface Result {
        /** [jpeg], the frame at [timeMs] of the stream. */
        class Frame(val jpeg: PreviewJpeg, val timeMs: Long) : Result

        /** No frame in time (or none that fits a cast message): the step has none. */
        data object NoFrame : Result

        /** The stream can't be played (or no offscreen surface): no frame of it will come. */
        data object Failed : Result
    }

    private class Grab(val startedAtNs: Long, val windowUs: LongRange, val onResult: (Result) -> Unit)

    /** A frame the decoder sent to the surface: when, and its media time. */
    private class Released(val releaseTimeNs: Long, val presentationTimeUs: Long)

    private val thread = HandlerThread("CastSeekPreview").apply { start() }
    private val handler = Handler(thread.looper)
    private val giveUp = Runnable { finish(Result.NoFrame, releasePlayer = true) }

    private var reader: OffscreenFrameReader? = null
    private var player: ExoPlayer? = null
    private var sourceFactory: (() -> MediaSource)? = null
    private var grab: Grab? = null
    private var aspectRatio = 16f / 9f

    // The last frame the decoder sent to the surface (written on the playback thread)
    private val lastReleased = AtomicReference<Released?>(null)

    /** The stream to decode from now on, [factory] creating its media source; drops the grab in flight. */
    fun setSource(factory: () -> MediaSource) {
        handler.post {
            cancelGrab()
            releasePlayer()
            sourceFactory = factory
            aspectRatio = 16f / 9f
        }
    }

    /**
     * Decodes a frame at [positionMs] — any within [toleranceMs] of it, a keyframe when there's one,
     * saving the decode up to the exact time. [onResult] is called on the grabber's thread, once.
     */
    fun grab(positionMs: Long, toleranceMs: Long, onResult: (Result) -> Unit) {
        handler.post { startGrab(positionMs, toleranceMs, onResult) }
    }

    /**
     * The stream's duration once its player has read it, null when it can't be read within
     * [PREVIEW_FETCH_TIMEOUT_MS]. [onResult] is called on the grabber's thread, once.
     */
    fun duration(onResult: (Long?) -> Unit) {
        handler.post {
            val frameReader = reader ?: OffscreenFrameReader.create(handler, ::onFrameAvailable)?.also { reader = it }
            val previewPlayer = frameReader?.let { player ?: createPlayer(it) }
            if (previewPlayer == null) {
                onResult(null)
                return@post
            }
            if (previewPlayer.playbackState == Player.STATE_IDLE) previewPlayer.prepare()
            val deadline = SystemClock.elapsedRealtime() + PREVIEW_FETCH_TIMEOUT_MS
            val poll = object : Runnable {
                override fun run() {
                    // Failed or dropped meanwhile: no duration will come
                    if (player !== previewPlayer || previewPlayer.playerError != null) return onResult(null)
                    val durationMs = previewPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0L }
                    when {
                        durationMs != null -> onResult(durationMs)
                        SystemClock.elapsedRealtime() >= deadline -> onResult(null)
                        else -> handler.postDelayed(this, DURATION_POLL_MS)
                    }
                }
            }
            poll.run()
        }
    }

    /** Frees the player, the GL surface and the thread. */
    fun release() {
        handler.post {
            cancelGrab()
            releasePlayer()
            reader?.release()
            reader = null
            thread.quitSafely()
        }
    }

    private fun startGrab(positionMs: Long, toleranceMs: Long, onResult: (Result) -> Unit) {
        cancelGrab()
        val frameReader = reader ?: OffscreenFrameReader.create(handler, ::onFrameAvailable)?.also { reader = it }
        if (frameReader == null) {
            onResult(Result.Failed)
            return
        }
        val previewPlayer = player ?: createPlayer(frameReader)
        if (previewPlayer == null) {
            onResult(Result.Failed)
            return
        }
        // The seek lands within the tolerance, or on the next keyframe in a stream without a sync-point
        // index (a progressive MPEG-TS): a frame far from the step is one an earlier seek left behind
        val windowMs = (positionMs - toleranceMs - FRAME_WINDOW_BEFORE_MS)..(positionMs + toleranceMs + FRAME_WINDOW_AFTER_MS)
        grab = Grab(System.nanoTime(), windowMs.first * 1000..windowMs.last * 1000, onResult)
        handler.postDelayed(giveUp, PREVIEW_FETCH_TIMEOUT_MS)
        Log.d(TAG, "Grab at $positionMs ms, ±$toleranceMs ms")
        val toleranceUs = toleranceMs * 1000
        previewPlayer.setSeekParameters(SeekParameters(toleranceUs, toleranceUs))
        previewPlayer.seekTo(positionMs)
        if (previewPlayer.playbackState == Player.STATE_IDLE) previewPlayer.prepare()
    }

    private fun onFrameAvailable() {
        val frameReader = reader ?: return
        // Always latched, or the decoder runs out of buffers
        frameReader.latch()
        val current = grab ?: return
        // The first frame the decoder released since the grab started, near the time asked: the one after the seek
        val released = lastReleased.get()
        if (released == null || released.releaseTimeNs < current.startedAtNs) {
            Log.v(TAG, "Frame of an earlier seek skipped")
            return
        }
        if (released.presentationTimeUs !in current.windowUs) {
            Log.v(TAG, "Frame at ${released.presentationTimeUs / 1000} ms skipped, out of the step")
            return
        }
        val (width, height) = previewFrameSize(aspectRatio)
        val bitmap = runCatching { frameReader.read(width, height) }
            .onFailure { Log.w(TAG, "Frame read back failed", it) }
            .getOrNull()
        if (bitmap == null) {
            finish(Result.Failed, releasePlayer = true)
            return
        }
        val jpeg = CastPreviewProtocol.encodeWithinBudget { quality ->
            ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
        }
        bitmap.recycle()
        val result = if (jpeg != null) Result.Frame(PreviewJpeg(jpeg, width, height), released.presentationTimeUs / 1000) else Result.NoFrame
        finish(result, releasePlayer = false)
    }

    private fun finish(result: Result, releasePlayer: Boolean) {
        val current = grab ?: return
        Log.d(
            TAG,
            "Grab ended: ${result::class.simpleName}, after ${(System.nanoTime() - current.startedAtNs) / 1_000_000} ms" +
                (lastReleased.get()?.let { ", frame at ${it.presentationTimeUs / 1000} ms" } ?: ""),
        )
        grab = null
        handler.removeCallbacks(giveUp)
        // A late frame mustn't be taken for the next step's: a new player for it
        if (releasePlayer) releasePlayer()
        current.onResult(result)
    }

    private fun cancelGrab() {
        grab = null
        handler.removeCallbacks(giveUp)
    }

    private fun createPlayer(frameReader: OffscreenFrameReader): ExoPlayer? {
        val mediaSource = runCatching { sourceFactory?.invoke() }
            .onFailure { Log.w(TAG, "Preview source creation failed", it) }
            .getOrNull() ?: return null
        val previewPlayer = ExoPlayer.Builder(context, previewRenderersFactory(context))
            .setLooper(thread.looper)
            .setBandwidthMeter(DefaultBandwidthMeter.Builder(context).build())
            .setLoadControl(previewLoadControl())
            .build()
        previewPlayer.trackSelectionParameters = previewTrackSelectionParameters(previewPlayer.trackSelectionParameters)
        previewPlayer.volume = 0f
        previewPlayer.playWhenReady = false
        // Coalesces the seeks issued while one is still loading or decoding
        previewPlayer.setScrubbingModeEnabled(true)
        previewPlayer.setVideoFrameMetadataListener { presentationTimeUs, releaseTimeNs, _, _ ->
            // Playback thread: the frame is about to be queued to the surface
            lastReleased.set(Released(releaseTimeNs, presentationTimeUs))
        }
        previewPlayer.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspectRatio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w(TAG, "Cast seekbar preview failed", error)
                // Released out of its own callback
                handler.post {
                    if (player !== previewPlayer) return@post
                    if (grab != null) finish(Result.Failed, releasePlayer = true) else releasePlayer()
                }
            }
        })
        previewPlayer.setVideoSurface(frameReader.surface)
        previewPlayer.setMediaSource(mediaSource)
        player = previewPlayer
        return previewPlayer
    }

    private fun releasePlayer() {
        player?.release()
        player = null
        lastReleased.set(null)
    }

    private companion object {
        const val TAG = "CastFrameGrabber"

        /** Beyond the seek tolerance, how far before the time asked a frame may be. */
        const val FRAME_WINDOW_BEFORE_MS = 1_000L

        /** Beyond the seek tolerance, how far after: up to a keyframe interval, for streams seeking by bytes. */
        const val FRAME_WINDOW_AFTER_MS = 10_000L

        /** How often [duration] looks at the player while it reads the stream. */
        const val DURATION_POLL_MS = 50L
    }
}
