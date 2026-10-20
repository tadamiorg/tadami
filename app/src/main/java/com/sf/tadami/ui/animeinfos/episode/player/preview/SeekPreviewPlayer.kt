package com.sf.tadami.ui.animeinfos.episode.player.preview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.TextureView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer

/**
 * A second, muted ExoPlayer that shows the frame under the seekbar thumb while the user drags it.
 *
 * It reads the stream through the main player's media source factory (headers, HLS fixes, cache) but has
 * its own bandwidth meter, track selection and load control, so it never changes what the main player
 * buffers or picks. Its loads wait while [canLoad] says the main player needs the network. It is created
 * on the first scrub and released with [release]. Main thread only.
 */
@UnstableApi
class SeekPreviewPlayer(
    private val context: Context,
    private val canLoad: () -> Boolean,
) {

    private var player: ExoPlayer? = null
    private var sourceKey: String? = null
    private var sourceFactory: (() -> MediaSource)? = null
    private var textureView: TextureView? = null

    private val throttle = LatestThrottle(SEEK_INTERVAL_MS)
    private val handler = Handler(Looper.getMainLooper())
    private val flushSeek = Runnable { pumpSeek() }

    /** A frame was drawn since the preview surface was attached; until then the UI shows a placeholder. */
    var hasFrame by mutableStateOf(false)
        private set

    /** Display aspect ratio of the preview frames. */
    var videoAspectRatio by mutableFloatStateOf(DEFAULT_ASPECT_RATIO)
        private set

    /** The current source can't be previewed (the preview player failed on it): the UI shows no frame. */
    var failed by mutableStateOf(false)
        private set

    /** Sets the stream to preview, [key] identifying it; a new stream drops the previous preview player. */
    fun setSource(key: String?, factory: (() -> MediaSource)?) {
        if (key == sourceKey) return
        release()
        sourceKey = key
        sourceFactory = factory
        failed = false
    }

    /** Shows the frame at [positionMs]: the newest position wins, at most one seek per [SEEK_INTERVAL_MS]. */
    fun scrubTo(positionMs: Long) {
        if (failed || sourceFactory == null) return
        throttle.push(positionMs)
        pumpSeek()
    }

    /** The drag ended: drops the seek still waiting. The player is kept for the next drag. */
    fun endScrub() {
        handler.removeCallbacks(flushSeek)
        throttle.clear()
    }

    fun attachSurface(view: TextureView) {
        textureView = view
        hasFrame = false
        player?.setVideoTextureView(view)
    }

    fun detachSurface(view: TextureView) {
        if (textureView !== view) return
        player?.clearVideoTextureView(view)
        textureView = null
        hasFrame = false
    }

    /** Frees the decoder, the loads and the playback thread; the next scrub creates a new player. */
    fun release() {
        endScrub()
        player?.release()
        player = null
        hasFrame = false
    }

    private fun pumpSeek() {
        handler.removeCallbacks(flushSeek)
        val now = SystemClock.uptimeMillis()
        val delay = if (canLoad()) {
            throttle.poll(now)?.let(::seek)
            throttle.delayMs(now)
        } else {
            WAIT_FOR_MAIN_PLAYER_MS
        }
        if (throttle.hasPending && delay > 0L) handler.postDelayed(flushSeek, delay)
    }

    private fun seek(positionMs: Long) {
        val previewPlayer = player
        // Scrubbing mode coalesces the seeks issued while one is still loading or decoding
        if (previewPlayer != null) previewPlayer.seekTo(positionMs) else create(positionMs)
    }

    private fun create(startPositionMs: Long) {
        val mediaSource = runCatching { sourceFactory?.invoke() }
            .onFailure { Log.w(TAG, "Preview source creation failed", it) }
            .getOrNull() ?: return
        val previewPlayer = ExoPlayer.Builder(context, videoOnlyRenderers(context))
            // Its own meter: preview loads must not move the main player's bitrate estimate
            .setBandwidthMeter(DefaultBandwidthMeter.Builder(context).build())
            .setLoadControl(
                DefaultLoadControl.Builder()
                    // Loads about one segment around the target, nothing more
                    .setBufferDurationsMs(MIN_BUFFER_MS, MAX_BUFFER_MS, MIN_BUFFER_MS, MIN_BUFFER_MS)
                    .setTargetBufferBytes(TARGET_BUFFER_BYTES)
                    .setPrioritizeTimeOverSizeThresholds(false)
                    .setBackBuffer(0, false)
                    .build()
            )
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .build()
        previewPlayer.trackSelectionParameters = previewTrackSelectionParameters(previewPlayer.trackSelectionParameters)
        previewPlayer.volume = 0f
        previewPlayer.playWhenReady = false
        previewPlayer.setScrubbingModeEnabled(true)
        previewPlayer.addListener(object : Player.Listener {
            override fun onRenderedFirstFrame() {
                hasFrame = true
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Not retried on every drag: the preview stays off until the source changes
                Log.w(TAG, "Seekbar preview failed", error)
                failed = true
                handler.post { release() }
            }
        })
        textureView?.let(previewPlayer::setVideoTextureView)
        previewPlayer.setMediaSource(mediaSource, startPositionMs)
        previewPlayer.prepare()
        player = previewPlayer
    }

    private companion object {
        const val TAG = "SeekPreviewPlayer"
        const val SEEK_INTERVAL_MS = 100L
        const val WAIT_FOR_MAIN_PLAYER_MS = 250L
        const val MIN_BUFFER_MS = 500
        const val MAX_BUFFER_MS = 1_000
        const val TARGET_BUFFER_BYTES = 4 * 1024 * 1024
        const val DEFAULT_ASPECT_RATIO = 16f / 9f

        /** A single video renderer: no audio, text nor metadata renderer is even created. */
        fun videoOnlyRenderers(context: Context) = RenderersFactory { handler, videoListener, _, _, _ ->
            arrayOf(
                MediaCodecVideoRenderer.Builder(context)
                    .setEventHandler(handler)
                    .setEventListener(videoListener)
                    .setEnableDecoderFallback(true)
                    .build()
            )
        }
    }
}
