package com.sf.tadami.ui.animeinfos.episode.player.preview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Frames of the seekbar preview: one per preview step (see [previewBucketSizeMs]), kept per stream.
 *
 * A step's frame comes from a second, muted ExoPlayer that reads the stream through the main player's media
 * source factory (headers, HLS fixes, cache) but has its own bandwidth meter, track selection and load
 * control, so it never changes what the main player buffers or picks. It decodes into a hidden
 * [TextureView] ([attachSurface]), the frame is scaled into a bitmap and cached: a step already seen shows
 * at once, with no new load nor decode.
 *
 * With [setPrefetch], every step is fetched in the background once the main player plays steadily
 * ([mainPlayback], [PreviewPrefetch]), nearest its position first, one at a time with a gap, so a scrub
 * finds its frame cached — while [canPrefetch] allows it (a stream: the "Only on Wi-Fi" setting, the main
 * player buffered ahead). A scrub's step comes first: it takes over a prefetch loading another step. Main thread
 * only.
 */
@UnstableApi
class SeekPreviewPlayer(
    private val context: Context,
    private val canLoad: () -> Boolean,
    /** The main player's playback, null while the screen isn't in the foreground: the prefetch waits. */
    private val mainPlayback: () -> MainPlayback? = { null },
    /** Whether a prefetch may load now ([previewPrefetchMayLoad]); asked before each. */
    private val canPrefetch: () -> Boolean = { true },
) : TextureView.SurfaceTextureListener {

    /** The main player's [state], [positionMs] and [durationMs]. */
    data class MainPlayback(val state: PreviewPrefetch.Playback?, val positionMs: Long, val durationMs: Long)

    private var player: ExoPlayer? = null
    private var sourceFactory: (() -> MediaSource)? = null
    private var textureView: TextureView? = null
    private var surface: Surface? = null

    // 256×144 RGB_565 frames (72 KiB): 24 MiB keep 341 of them, every step of a 2 h 50 film
    private val frames = PreviewFrameCache<Bitmap>(PREVIEW_BITMAP_CACHE_BYTES) { it.allocationByteCount.toLong() }
    private val requests = BucketRequests()

    private val schedule = PreviewPrefetch(SystemClock::elapsedRealtime)
    private val prefetchTick = Runnable { onPrefetchTick() }
    private var prefetchEnabled = false
    private var prefetchGapMs = PREVIEW_PREFETCH_GAP_LOCAL_MS

    // What the preview player's loads read for this stream, cache hits included (the logs' rough data cost)
    private var previewBytes = 0L

    // Frames dropped on memory pressure: not all made again for this stream
    private var prefetchSuspended = false
    private var prefetchStartedAt: Long? = null
    private var prefetchComplete = false
    private var frameBytes = PREVIEW_FRAME_BYTES

    // Steps that gave no frame in time: not fetched again for this stream
    private val unavailable = mutableSetOf<Int>()
    private val handler = Handler(Looper.getMainLooper())
    private val pump = Runnable { pumpRequests() }
    private val giveUp = Runnable { onFetchTimeout() }

    private var durationMs = 0L
    private var fingerBucket: Int? = null
    private var videoAspectRatio = DEFAULT_ASPECT_RATIO
    private var fetchStartedAtNs = 0L
    private var fetchStartedAt = 0L

    // Media times of the fetch's step: a frame elsewhere is one an earlier seek left behind
    private var fetchWindowUs = LongRange.EMPTY

    // The decoder released a frame since the fetch started: the next texture update is the step's frame
    private var frameReleasedSinceFetch = false

    // The fetch's step, and the time and release time of the frame the decoder released in it (latency logs)
    private var fetchRangeMs = LongRange.EMPTY
    private var releasedFramePtsUs = 0L
    private var releasedFrameAtNs = 0L

    // When the finger reached its step, whether the step was cached then, and whether its frame was drawn (latency logs)
    private var fingerArrivedAt = 0L
    private var fingerArrivedCached = false
    private var fingerStepDrawn = false

    // The step of the frame shown
    private var shownBucket: Int? = null

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
        shownBucket = null
        frameLoading = false
        schedule.reset()
        prefetchSuspended = false
        prefetchStartedAt = null
        prefetchComplete = false
        previewBytes = 0L
        if (prefetchEnabled) scheduleTick(0L)
    }

    /**
     * Whether every step of the stream is fetched in the background, [gapMs] apart. Off, a prefetch loading
     * still lands, nothing more starts.
     */
    fun setPrefetch(enabled: Boolean, gapMs: Long = PREVIEW_PREFETCH_GAP_LOCAL_MS) {
        prefetchGapMs = gapMs
        if (enabled == prefetchEnabled) return
        prefetchEnabled = enabled
        if (enabled) scheduleTick(0L) else handler.removeCallbacks(prefetchTick)
    }

    /** Shows the frame of the step [positionMs] falls in, fetching it unless it's cached or loading. */
    fun scrubTo(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L || frames.sourceId == null) return
        this.durationMs = durationMs
        val bucket = previewBucketOf(positionMs, durationMs)
        if (bucket == fingerBucket) return
        fingerBucket = bucket
        fingerArrivedAt = SystemClock.elapsedRealtime()
        fingerArrivedCached = key(bucket) in frames
        fingerStepDrawn = false
        val status = when {
            fingerArrivedCached -> "cached"
            failed || bucket in unavailable -> "no frame"
            else -> "loading"
        }
        Log.d(TAG, "Finger on step $bucket: $status")
        requests.aim(bucket)
        showBucket(bucket)
        pumpRequests()
    }

    /**
     * The thumbnail was drawn, [exact] when with the frame of the step under the finger: logs the time since
     * the finger reached that step, once per step.
     */
    fun onThumbnailDrawn(exact: Boolean) {
        val bucket = fingerBucket ?: return
        if (!exact || fingerStepDrawn || shownBucket != bucket) return
        fingerStepDrawn = true
        Log.d(
            TAG,
            "Finger's step $bucket drawn in ${SystemClock.elapsedRealtime() - fingerArrivedAt} ms " +
                if (fingerArrivedCached) "(cached)" else "(fetched)",
        )
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
        handler.removeCallbacks(prefetchTick)
        prefetchEnabled = false
        release()
        frames.setSource(null)
        frame = null
        shownBucket = null
    }

    fun onTrimMemory(level: Int) {
        if (!shouldDropPreviewFrames(level)) return
        frames.clear()
        prefetchSuspended = true
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
        if (cached != null) {
            frame = cached
            shownBucket = bucket
        }
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

    private fun scheduleTick(delayMs: Long) {
        handler.removeCallbacks(prefetchTick)
        handler.postDelayed(prefetchTick, delayMs)
    }

    private fun onPrefetchTick() {
        if (!prefetchEnabled || frames.sourceId == null || failed || prefetchSuspended || prefetchComplete) return
        val main = mainPlayback()
        schedule.onStatus(main?.state, main?.positionMs ?: schedule.positionMs)
        val duration = main?.durationMs?.takeIf { it > 0L } ?: durationMs
        // A stream waits while the network can't be spared (Wi-Fi only off Wi-Fi, main player short of buffer)
        if (surface != null && sourceFactory != null && duration > 0L && canPrefetch()) {
            when (val next = nextPreviewPrefetch(schedule, requests, duration, ::isDone, frames.hasRoomFor(frameBytes))) {
                is PreviewPrefetchStep.Fetch -> if (requests.prefetch(next.bucket)) {
                    durationMs = duration
                    if (prefetchStartedAt == null) prefetchStartedAt = SystemClock.elapsedRealtime()
                    fetch(next.bucket)
                }
                PreviewPrefetchStep.Done -> {
                    prefetchComplete = true
                    val elapsed = prefetchStartedAt?.let { SystemClock.elapsedRealtime() - it } ?: 0L
                    Log.i(
                        TAG,
                        "Prefetch complete: ${frames.size} frames, ${unavailable.size} without, " +
                            "${frames.bytes / 1024} KiB, in ${elapsed / 1000} s, " +
                            "${previewBytes / 1024} KiB read by the preview player (cache hits included)",
                    )
                    return
                }
                PreviewPrefetchStep.Wait -> Unit
            }
        }
        scheduleTick(PREFETCH_TICK_MS)
    }

    /** A fetch of [bucket] ended; after a prefetch, the next one comes after a gap. */
    private fun onFetchEnded(bucket: Int) {
        val wasPrefetch = requests.inFlightIsPrefetch
        requests.finished(bucket)
        if (wasPrefetch) {
            schedule.onDecodeEnded(prefetchGapMs)
            if (prefetchEnabled) scheduleTick(prefetchGapMs)
        }
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
        Log.d(TAG, "Fetching step $bucket at $targetMs ms" + if (requests.inFlightIsPrefetch) " (prefetch)" else "")
        fetchStartedAtNs = System.nanoTime()
        fetchStartedAt = SystemClock.elapsedRealtime()
        // The seek lands in the step, or on the next keyframe in a stream seeking by bytes (a joined MPEG-TS)
        fetchWindowUs = (range.first - FRAME_WINDOW_BEFORE_MS) * 1000..(range.last + FRAME_WINDOW_AFTER_MS) * 1000
        fetchRangeMs = range
        frameReleasedSinceFetch = false
        // A scrub may take over a prefetch: its own time limit only
        handler.removeCallbacks(giveUp)
        handler.postDelayed(giveUp, PREVIEW_FETCH_TIMEOUT_MS)
        previewPlayer.setSeekParameters(SeekParameters(toleranceUs, toleranceUs))
        previewPlayer.seekTo(targetMs)
        if (previewPlayer.playbackState == Player.STATE_IDLE) previewPlayer.prepare()
    }

    private fun captureFrame() {
        val bucket = requests.inFlight ?: return
        if (!frameReleasedSinceFetch) return
        val view = textureView ?: return
        val height = (PREVIEW_FRAME_WIDTH_PX / videoAspectRatio).roundToInt().coerceAtLeast(1)
        val bitmap = view.getBitmap(PREVIEW_FRAME_WIDTH_PX, height)?.let(::toPreviewFrame) ?: return
        frameBytes = bitmap.allocationByteCount.toLong()
        frames.put(key(bucket), bitmap)
        val framePtsMs = releasedFramePtsUs / 1000
        val textureMatches = view.surfaceTexture?.timestamp == releasedFrameAtNs
        Log.d(
            TAG,
            "Cached step $bucket in ${SystemClock.elapsedRealtime() - fetchStartedAt} ms: " +
                "${frames.size} frames, ${frames.bytes / 1024} KiB; " +
                "frame at ${framePtsMs - fetchRangeMs.first} ms into the step, " +
                "${(framePtsMs - fetchRangeMs.last - 1).coerceAtLeast(0L)} ms past its end, " +
                "released ${(releasedFrameAtNs - fetchStartedAtNs) / 1_000_000} ms after the seek, " +
                "texture ${if (textureMatches) "matches" else "differs"}",
        )
        handler.removeCallbacks(giveUp)
        onFetchEnded(bucket)
        fingerBucket?.let(::showBucket)
        pumpRequests()
    }

    private fun onFetchTimeout() {
        val bucket = requests.inFlight ?: return
        // A late frame mustn't be taken for the next step: drop the player, the next fetch makes a new one
        Log.w(
            TAG,
            "Step $bucket timed out, preview player state ${player?.playbackState} at ${player?.currentPosition} ms, " +
                "buffered ${player?.totalBufferedDuration} ms ahead, up to ${player?.bufferedPosition} ms",
        )
        player?.release()
        player = null
        unavailable += bucket
        onFetchEnded(bucket)
        fingerBucket?.let(::showBucket)
        pumpRequests()
    }

    /** Logs a load of the preview player: its bytes, its own length, and when it ended since the last fetch started. */
    private fun logLoad(outcome: String, info: LoadEventInfo, data: MediaLoadData) {
        Log.d(
            TAG,
            "Load $outcome for step ${requests.inFlight}: ${info.bytesLoaded} B in ${info.loadDurationMs} ms, " +
                "data type ${data.dataType}, ${SystemClock.elapsedRealtime() - fetchStartedAt} ms after the seek",
        )
    }

    private fun create(): ExoPlayer? {
        val mediaSource = runCatching { sourceFactory?.invoke() }
            .onFailure { Log.w(TAG, "Preview source creation failed", it) }
            .getOrNull() ?: return null
        val previewPlayer = ExoPlayer.Builder(context, previewRenderersFactory(context))
            // Its own meter: preview loads must not move the main player's bitrate estimate
            .setBandwidthMeter(DefaultBandwidthMeter.Builder(context).build())
            .setLoadControl(previewLoadControl())
            .build()
        Log.d(TAG, "Preview player created")
        previewPlayer.trackSelectionParameters = previewTrackSelectionParameters(previewPlayer.trackSelectionParameters)
        previewPlayer.volume = 0f
        previewPlayer.playWhenReady = false
        // Coalesces the seeks issued while one is still loading or decoding
        previewPlayer.setScrubbingModeEnabled(true)
        previewPlayer.setVideoFrameMetadataListener { presentationTimeUs, releaseTimeNs, _, _ ->
            // Playback thread: the frame is about to be queued to the surface
            handler.post {
                if (releaseTimeNs >= fetchStartedAtNs && presentationTimeUs in fetchWindowUs) {
                    frameReleasedSinceFetch = true
                    releasedFramePtsUs = presentationTimeUs
                    releasedFrameAtNs = releaseTimeNs
                }
            }
        }
        previewPlayer.addAnalyticsListener(object : AnalyticsListener {
            // Every load, finished after its frame was taken or cut short by the next seek
            override fun onLoadCompleted(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData,
            ) {
                previewBytes += loadEventInfo.bytesLoaded
                logLoad("completed", loadEventInfo, mediaLoadData)
            }

            override fun onLoadCanceled(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData,
            ) {
                previewBytes += loadEventInfo.bytesLoaded
                logLoad("canceled", loadEventInfo, mediaLoadData)
            }

            override fun onLoadError(
                eventTime: AnalyticsListener.EventTime,
                loadEventInfo: LoadEventInfo,
                mediaLoadData: MediaLoadData,
                error: IOException,
                wasCanceled: Boolean,
            ) {
                val code = (error as? HttpDataSource.InvalidResponseCodeException)?.responseCode
                Log.d(TAG, "Load failed for step ${requests.inFlight}: ${code?.let { "HTTP $it" } ?: error.javaClass.simpleName}")
            }

            // A software decoder taken as a fallback shows here
            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long,
            ) {
                Log.d(TAG, "Decoder $decoderName initialized in $initializationDurationMs ms")
            }

            override fun onDroppedSeeksWhileScrubbing(eventTime: AnalyticsListener.EventTime, droppedSeeks: Int) {
                Log.d(TAG, "$droppedSeeks seeks dropped while scrubbing")
            }
        })
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
        const val DEFAULT_ASPECT_RATIO = 16f / 9f

        /** How often the prefetch looks at the main player. */
        const val PREFETCH_TICK_MS = 1_000L


        /** Beyond the step, how far before a frame may be. */
        const val FRAME_WINDOW_BEFORE_MS = 1_000L

        /** Beyond the step, how far after: a keyframe interval of a stream seeking by bytes (Kwik's reach 14 s). */
        const val FRAME_WINDOW_AFTER_MS = 15_000L
    }
}
