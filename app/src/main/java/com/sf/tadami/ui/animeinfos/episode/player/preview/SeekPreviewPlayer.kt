package com.sf.tadami.ui.animeinfos.episode.player.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.getValue
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
import kotlin.math.roundToInt

/**
 * Frames of the seekbar preview: one per preview step (see [previewBucketSizeMs]), kept per stream.
 *
 * A step's frame comes from a second, muted ExoPlayer that reads the stream through the main player's media
 * source factory (headers, HLS fixes, cache) but has its own bandwidth meter, track selection and load
 * control, so it never changes what the main player buffers or picks. It decodes into a hidden
 * [TextureView] ([attachSurface]), the frame is scaled into a bitmap and cached: a step already seen shows
 * at once, with no new load nor decode. Main thread only.
 */
@UnstableApi
class SeekPreviewPlayer(
    private val context: Context,
    private val canLoad: () -> Boolean,
) : TextureView.SurfaceTextureListener {

    private var player: ExoPlayer? = null
    private var sourceFactory: (() -> MediaSource)? = null
    private var textureView: TextureView? = null
    private var surface: Surface? = null

    private val frames = PreviewFrameCache<Bitmap>(FRAME_CACHE_BYTES) { it.allocationByteCount.toLong() }
    private val requests = BucketRequests()

    // Steps that gave no frame in time: not fetched again for this stream
    private val unavailable = mutableSetOf<Int>()
    private val handler = Handler(Looper.getMainLooper())
    private val pump = Runnable { pumpRequests() }
    private val giveUp = Runnable { onFetchTimeout() }

    private var durationMs = 0L
    private var fingerBucket: Int? = null
    private var videoAspectRatio = DEFAULT_ASPECT_RATIO
    private var fetchStartedAtNs = 0L

    // The decoder released a frame since the fetch started: the next texture update is the step's frame
    private var frameReleasedSinceFetch = false

    /** The frame shown: the one of the step under the finger, or the last one shown while it loads. */
    var frame by mutableStateOf<Bitmap?>(null)
        private set

    /** The frame of the step under the finger is loading: [frame] is an older one meanwhile, or none. */
    var frameLoading by mutableStateOf(false)
        private set

    /** The current stream can't be previewed (the preview player failed on it): no new frame is fetched. */
    var failed by mutableStateOf(false)
        private set

    /** Sets the stream to preview, [sourceId] being its [previewSourceIdentity]; another stream drops the frames. */
    fun setSource(sourceId: String?, factory: (() -> MediaSource)?) {
        if (sourceId == frames.sourceId) return
        release()
        frames.setSource(sourceId)
        unavailable.clear()
        sourceFactory = factory
        failed = false
        frame = null
        frameLoading = false
    }

    /** Shows the frame of the step [positionMs] falls in, fetching it unless it's cached or loading. */
    fun scrubTo(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L || frames.sourceId == null) return
        this.durationMs = durationMs
        val bucket = previewBucketOf(positionMs, durationMs)
        if (bucket == fingerBucket) return
        fingerBucket = bucket
        requests.aim(bucket)
        showBucket(bucket)
        pumpRequests()
    }

    /** The drag ended: nothing new is fetched, the fetch in flight still lands in the cache. */
    fun endScrub() {
        fingerBucket = null
        requests.aim(null)
        handler.removeCallbacks(pump)
    }

    /** Frees the decoder, the loads and the playback thread. The frames are kept. */
    fun release() {
        endScrub()
        handler.removeCallbacks(giveUp)
        requests.clear()
        player?.release()
        player = null
    }

    /** The player screen is gone: releases everything, frames included. */
    fun destroy() {
        release()
        frames.setSource(null)
        frame = null
    }

    fun onTrimMemory(level: Int) {
        if (shouldDropPreviewFrames(level)) frames.clear()
    }

    fun attachSurface(view: TextureView) {
        textureView = view
        view.surfaceTextureListener = this
        view.surfaceTexture?.let { onSurfaceTextureAvailable(it, view.width, view.height) }
    }

    fun detachSurface(view: TextureView) {
        if (textureView !== view) return
        view.surfaceTextureListener = null
        releaseSurface()
        textureView = null
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        surface = Surface(surfaceTexture).also { player?.setVideoSurface(it) }
        pumpRequests()
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) = Unit

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        releaseSurface()
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        captureFrame()
    }

    private fun releaseSurface() {
        surface?.let {
            player?.clearVideoSurface(it)
            it.release()
        }
        surface = null
    }

    private fun key(bucket: Int) = PreviewFrameKey(frames.sourceId.orEmpty(), bucket)

    /** Cached, or known to give no frame: nothing to fetch. */
    private fun isDone(bucket: Int) = key(bucket) in frames || bucket in unavailable

    private fun showBucket(bucket: Int) {
        val cached = frames[key(bucket)]
        if (cached != null) frame = cached
        frameLoading = cached == null && !failed && bucket !in unavailable
    }

    private fun pumpRequests() {
        handler.removeCallbacks(pump)
        val target = fingerBucket ?: return
        if (failed || surface == null || sourceFactory == null || isDone(target)) return
        if (!canLoad()) {
            // Leaves the network to the main player while its buffer is short
            handler.postDelayed(pump, WAIT_FOR_MAIN_PLAYER_MS)
            return
        }
        requests.next(::isDone)?.let(::fetch)
    }

    private fun fetch(bucket: Int) {
        val range = previewBucketRangeMs(bucket, durationMs)
        val targetMs = (range.first + range.last + 1) / 2
        // Any frame of the step will do: a keyframe in it when there's one, saving the decode up to the middle
        val toleranceUs = (range.last + 1 - range.first) / 2 * 1000
        val previewPlayer = player ?: create()
        if (previewPlayer == null) {
            requests.finished(bucket)
            return
        }
        Log.d(TAG, "Fetching step $bucket at $targetMs ms")
        fetchStartedAtNs = System.nanoTime()
        frameReleasedSinceFetch = false
        handler.postDelayed(giveUp, FETCH_TIMEOUT_MS)
        previewPlayer.setSeekParameters(SeekParameters(toleranceUs, toleranceUs))
        previewPlayer.seekTo(targetMs)
        if (previewPlayer.playbackState == Player.STATE_IDLE) previewPlayer.prepare()
    }

    private fun captureFrame() {
        val bucket = requests.inFlight ?: return
        if (!frameReleasedSinceFetch) return
        val view = textureView ?: return
        val height = (FRAME_WIDTH_PX / videoAspectRatio).roundToInt().coerceAtLeast(1)
        val bitmap = view.getBitmap(FRAME_WIDTH_PX, height) ?: return
        frames.put(key(bucket), bitmap)
        Log.d(TAG, "Cached step $bucket: ${frames.size} frames, ${frames.bytes / 1024} KiB")
        handler.removeCallbacks(giveUp)
        requests.finished(bucket)
        fingerBucket?.let(::showBucket)
        pumpRequests()
    }

    private fun onFetchTimeout() {
        val bucket = requests.inFlight ?: return
        // A late frame mustn't be taken for the next step: drop the player, the next fetch makes a new one
        Log.w(TAG, "Step $bucket timed out, preview player state ${player?.playbackState} at ${player?.currentPosition} ms")
        player?.release()
        player = null
        unavailable += bucket
        requests.finished(bucket)
        fingerBucket?.let(::showBucket)
        pumpRequests()
    }

    private fun create(): ExoPlayer? {
        val mediaSource = runCatching { sourceFactory?.invoke() }
            .onFailure { Log.w(TAG, "Preview source creation failed", it) }
            .getOrNull() ?: return null
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
            .build()
        previewPlayer.trackSelectionParameters = previewTrackSelectionParameters(previewPlayer.trackSelectionParameters)
        previewPlayer.volume = 0f
        previewPlayer.playWhenReady = false
        // Coalesces the seeks issued while one is still loading or decoding
        previewPlayer.setScrubbingModeEnabled(true)
        previewPlayer.setVideoFrameMetadataListener { _, releaseTimeNs, _, _ ->
            // Playback thread: the frame is about to be queued to the surface
            handler.post { if (releaseTimeNs >= fetchStartedAtNs) frameReleasedSinceFetch = true }
        }
        previewPlayer.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Not retried on every drag: no new frame until the source changes
                Log.w(TAG, "Seekbar preview failed", error)
                failed = true
                frameLoading = false
                handler.post { release() }
            }
        })
        surface?.let(previewPlayer::setVideoSurface)
        previewPlayer.setMediaSource(mediaSource)
        player = previewPlayer
        return previewPlayer
    }

    private companion object {
        const val TAG = "SeekPreviewPlayer"
        const val WAIT_FOR_MAIN_PLAYER_MS = 250L
        const val FETCH_TIMEOUT_MS = 10_000L
        const val MIN_BUFFER_MS = 500
        const val MAX_BUFFER_MS = 1_000
        const val TARGET_BUFFER_BYTES = 4 * 1024 * 1024
        const val DEFAULT_ASPECT_RATIO = 16f / 9f

        // 256×144 ARGB frames (144 KiB): 12 MiB keep about 85 of them, most of an episode at 1% steps
        const val FRAME_WIDTH_PX = 256
        const val FRAME_CACHE_BYTES = 12L * 1024 * 1024

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
