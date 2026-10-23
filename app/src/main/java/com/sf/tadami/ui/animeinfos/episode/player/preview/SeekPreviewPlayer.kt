package com.sf.tadami.ui.animeinfos.episode.player.preview

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.sf.tadami.data.download.EpisodePreviews
import com.sf.tadami.ui.animeinfos.episode.cast.preview.CastPreviewProtocol
import com.sf.tadami.ui.animeinfos.episode.cast.preview.previewFrameSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Frames of the seekbar preview of a downloaded episode: one per preview step (see [previewBucketSizeMs]), kept
 * per file. A streamed episode has none: its seekbar shows the time alone.
 *
 * A step's frame comes from a second, muted ExoPlayer that reads the episode's file, with its own bandwidth
 * meter, track selection and load control, so it never changes what the main player buffers or picks. It
 * decodes into a hidden [TextureView] ([attachSurface]), the frame is scaled into a bitmap and cached: a step
 * already seen shows at once, with no new load nor decode.
 *
 * The frames kept next to the episode ([setPreviews], made after its download, see PreviewGenerator) are read
 * off the main thread as it opens, so every step is ready before the first scrub; nothing is decoded for them.
 *
 * With [setPrefetch], every step is fetched in the background once the main player plays steadily
 * ([mainPlayback], [PreviewPrefetch]), nearest its position first, one at a time with a gap, so a scrub
 * finds its frame cached. A scrub's step comes first: it takes over a prefetch loading another step. Main thread
 * only, [scope] being the main thread's.
 */
@UnstableApi
class SeekPreviewPlayer(
    private val context: Context,
    private val scope: CoroutineScope,
    /** The main player's playback, null while the screen isn't in the foreground: the prefetch waits. */
    private val mainPlayback: () -> MainPlayback? = { null },
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

    private val schedule = phonePreviewPrefetch(SystemClock::elapsedRealtime)
    private val prefetchTick = Runnable { onPrefetchTick() }
    private var prefetchEnabled = false

    // What the preview player's loads read from this file (logs)
    private var previewBytes = 0L

    // Frames dropped on memory pressure: not all made again for this stream
    private var prefetchSuspended = false

    // The last trim that dropped the frames came from memory pressure, not from the app going to the background
    private var trimUnderPressure = false
    private var prefetchStartedAt: Long? = null
    private var prefetchComplete = false
    private var frameBytes = PREVIEW_FRAME_BYTES

    // Steps that gave no frame in time: not fetched again for this stream
    private val unavailable = mutableSetOf<Int>()

    // The episode's frames kept next to it: read once the file is set, put in the cache once the duration is known
    private var previews: EpisodePreviews? = null
    private var sidecarJob: Job? = null
    private var sidecarLoading = false
    private var loadedSidecar: LoadedSidecar? = null
    private var sidecarApplied = false
    private var sidecarSaved = false

    // The time of each cached step's frame, for the sidecar
    private val frameTimesMs = mutableMapOf<Int, Long>()

    /** A sidecar's frames, decoded, by the sidecar's own steps; [readMs] what reading and decoding took. */
    private class LoadedSidecar(val sidecar: PreviewSidecar, val bitmaps: Map<Int, Bitmap>, val readMs: Long)
    private val handler = Handler(Looper.getMainLooper())
    private val pump = Runnable { pumpRequests() }
    private val giveUp = Runnable { onFetchTimeout() }
    private val delayedRelease = Runnable { release() }

    private var durationMs = 0L
    // A state: the thumbnail reads it when drawn, so a gesture's first step, set after the thumbnail's first
    // draw, draws again and logs its latency
    private var fingerBucket by mutableStateOf<Int?>(null)
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

    // The current file can't be previewed (the preview player failed on it too often): no new frame is fetched
    private var failed = false
    private val errors = PreviewErrors()

    /**
     * Changes whenever [frameAt] may answer otherwise: a frame cached or dropped, a step given up, the file
     * failed or changed. Read it in composition to show [frameAt] again.
     */
    var framesVersion by mutableIntStateOf(0)
        private set

    /**
     * What the thumbnail of [positionMs] shows in a [durationMs]-long video: the frame of its step, or while
     * that one is missing the cached frame of the nearest step within [previewNearestMaxSteps]; nothing without
     * a file (a stream, nothing playing) or a duration. Never a frame of another file. Not a state read: key it
     * on [framesVersion].
     */
    fun frameAt(positionMs: Long, durationMs: Long): PreviewShown {
        if (durationMs <= 0L || frames.sourceId == null) return PreviewShown.NONE
        val bucket = phonePreviewBucketOf(positionMs, durationMs)
        val shown = nearestCachedBucket(bucket, durationMs)
        return PreviewShown(
            bucket = bucket,
            // The chosen frame only is read: a read makes it the most recently used
            frame = shown?.let { frames[key(it)] },
            exact = shown == bucket,
            loading = shown != bucket && !failed && bucket !in unavailable,
        )
    }

    /** What the thumbnail shows for the step [bucket]; [exact] when [frame] is that step's own. */
    data class PreviewShown(val bucket: Int?, val frame: Bitmap?, val exact: Boolean, val loading: Boolean) {
        companion object {
            val NONE = PreviewShown(bucket = null, frame = null, exact = false, loading = false)
        }
    }

    /**
     * Sets the downloaded episode to preview, [sourceId] being its file, or none (a stream, nothing playing).
     * Another file drops the frames.
     */
    fun setSource(sourceId: String?, factory: (() -> MediaSource)?) {
        if (sourceId == frames.sourceId) return
        release()
        frames.setSource(sourceId)
        unavailable.clear()
        sidecarJob?.cancel()
        previews = null
        sidecarLoading = false
        loadedSidecar = null
        sidecarApplied = false
        sidecarSaved = false
        frameTimesMs.clear()
        sourceFactory = factory
        failed = false
        framesVersion++
        schedule.reset()
        prefetchSuspended = false
        prefetchStartedAt = null
        prefetchComplete = false
        previewBytes = 0L
        errors.reset()
        // The previous file's duration would give this file's frames the wrong steps
        durationMs = 0L
        if (prefetchEnabled) scheduleTick(0L)
    }

    /**
     * Where the frames of the downloaded episode [sourceId] (the file of [setSource]) are kept: they are read now,
     * off the main thread, and put in the cache, every step at once, as soon as the main player's duration is known.
     */
    fun setPreviews(sourceId: String, previews: EpisodePreviews?) {
        if (sourceId != frames.sourceId || previews == null) return
        this.previews = previews
        loadSidecar()
    }

    /**
     * Whether every step of the file is fetched in the background, [PREVIEW_PREFETCH_GAP_LOCAL_MS] apart. Off, a
     * prefetch loading still lands, nothing more starts.
     */
    fun setPrefetch(enabled: Boolean) {
        if (enabled == prefetchEnabled) return
        prefetchEnabled = enabled
        if (enabled) scheduleTick(0L) else handler.removeCallbacks(prefetchTick)
    }

    /** Shows the frame of the step [positionMs] falls in, fetching it unless it's cached or loading. */
    fun scrubTo(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L || frames.sourceId == null) return
        this.durationMs = durationMs
        applySidecar()
        val bucket = phonePreviewBucketOf(positionMs, durationMs)
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
        val nearest = if (fingerArrivedCached) null else nearestCachedBucket(bucket, durationMs)
        Log.d(TAG, "Finger on step $bucket: $status" + (nearest?.let { ", nearest $it" } ?: ""))
        requests.aim(bucket)
        pumpRequests()
    }

    /**
     * The thumbnail of the step [shownBucket] was drawn, [exact] when with that step's own frame: logs the time
     * since the finger reached that step, once per step.
     */
    fun onThumbnailDrawn(shownBucket: Int?, exact: Boolean) {
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
        handler.removeCallbacks(delayedRelease)
        endScrub()
        handler.removeCallbacks(giveUp)
        requests.clear()
        player?.release()
        player = null
    }

    /**
     * The screen paused but is still seen (PiP): [release] in [IDLE_RELEASE_MS], so a short stint comes back with
     * the decoder ready, unless [cancelRelease] comes first.
     */
    fun releaseLater() {
        handler.removeCallbacks(delayedRelease)
        handler.postDelayed(delayedRelease, IDLE_RELEASE_MS)
    }

    /** The screen is back: the decoder is kept. */
    fun cancelRelease() {
        handler.removeCallbacks(delayedRelease)
    }

    /** The player screen is gone: releases everything, frames included. */
    fun destroy() {
        handler.removeCallbacks(prefetchTick)
        prefetchEnabled = false
        release()
        sidecarJob?.cancel()
        loadedSidecar = null
        frames.setSource(null)
        framesVersion++
    }

    fun onTrimMemory(level: Int) {
        if (!shouldDropPreviewFrames(level)) return
        frames.clear()
        loadedSidecar = null
        trimUnderPressure = isMemoryPressureTrim(level)
        framesVersion++
        prefetchSuspended = true
    }

    /**
     * The player screen is in front again: the frames a background trim dropped come back, from the sidecar kept
     * next to the episode, else by a new prefetch. Not after memory pressure, nor while memory is low.
     */
    fun onResume() {
        val restore = shouldRestorePreviewFrames(
            dropped = prefetchSuspended,
            downloaded = frames.sourceId != null,
            memoryPressure = trimUnderPressure,
            lowMemory = isLowOnMemory(),
        )
        if (!restore) return
        Log.i(TAG, "Getting back the preview frames a background trim dropped")
        prefetchSuspended = false
        prefetchComplete = false
        prefetchStartedAt = null
        sidecarApplied = false
        loadSidecar()
        framesVersion++
        if (prefetchEnabled) scheduleTick(0L)
    }

    private fun isLowOnMemory(): Boolean {
        val activityManager = context.getSystemService(ActivityManager::class.java) ?: return false
        return ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo).lowMemory
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

    /** [bucket], or the nearest step to it with a frame cached; looked up without changing what's evicted first. */
    private fun nearestCachedBucket(bucket: Int, durationMs: Long): Int? = nearestPreviewBucket(
        bucket = bucket,
        last = phonePreviewBucketOf(durationMs - 1, durationMs),
        maxSteps = previewNearestMaxSteps(durationMs),
    ) { key(it) in frames }

    private fun pumpRequests() {
        handler.removeCallbacks(pump)
        val target = fingerBucket ?: return
        if (failed || surface == null || sourceFactory == null || isDone(target)) return
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
        if (duration > 0L) {
            durationMs = duration
            applySidecar()
        }
        // The sidecar's frames come first: nothing is decoded before they are known
        if (surface != null && sourceFactory != null && duration > 0L && !sidecarLoading) {
            when (val next = nextPreviewPrefetch(schedule, requests, duration, ::isDone, frames.hasRoomFor(frameBytes))) {
                is PreviewPrefetchStep.Fetch -> if (requests.prefetch(next.bucket)) {
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
                            "${previewBytes / 1024} KiB read by the preview player",
                    )
                    savePrefetchedFrames(duration)
                    return
                }
                PreviewPrefetchStep.Wait -> Unit
            }
        }
        scheduleTick(PREFETCH_TICK_MS)
    }

    /** Reads the episode's sidecar off the main thread, its frames decoded as the cache keeps them. */
    private fun loadSidecar() {
        val target = previews ?: return
        val sourceId = frames.sourceId
        sidecarJob?.cancel()
        sidecarLoading = true
        val startedAt = SystemClock.elapsedRealtime()
        sidecarJob = scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                target.read()?.let { sidecar ->
                    val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
                    val bitmaps = sidecar.entries.mapNotNull { entry ->
                        val jpeg = entry.jpeg ?: return@mapNotNull null
                        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options)?.let { entry.step to it }
                    }.toMap()
                    LoadedSidecar(sidecar, bitmaps, SystemClock.elapsedRealtime() - startedAt)
                }
            }
            if (frames.sourceId != sourceId) return@launch
            sidecarLoading = false
            if (loaded == null) {
                Log.d(TAG, "No preview frames kept next to this episode: the prefetch makes them")
                return@launch
            }
            loadedSidecar = loaded
            applySidecar()
        }
    }

    /**
     * Puts the loaded sidecar's frames in the cache, each step of the main player's timeline taking the frame of its
     * time; kept for a later duration while the main player's isn't the sidecar's (another file was playing).
     */
    private fun applySidecar() {
        val loaded = loadedSidecar ?: return
        val duration = durationMs.takeIf { it > 0L } ?: return
        if (!loaded.sidecar.matchesDuration(duration)) {
            Log.d(TAG, "Preview frames kept for ${loaded.sidecar.durationMs} ms, the player plays $duration ms")
            return
        }
        loadedSidecar = null
        sidecarApplied = true
        val last = phonePreviewBucketOf(duration - 1, duration)
        for (step in 0..last) {
            val entry = loaded.sidecar.entryAt(previewBucketRangeMs(step, duration)) ?: continue
            val bitmap = loaded.bitmaps[entry.step]
            when {
                bitmap != null -> {
                    frames.put(key(step), bitmap)
                    frameTimesMs[step] = entry.timeMs
                }
                !entry.hasFrame -> unavailable += step
            }
        }
        framesVersion++
        Log.i(
            TAG,
            "Loaded ${frames.size} preview frames kept next to the episode, ${unavailable.size} steps without, " +
                "read and decoded in ${loaded.readMs} ms",
        )
    }

    /**
     * The prefetch is done: an episode without frames kept next to it (downloaded before them, or whose frames
     * couldn't be made after its download) gets its own now, every step of the [duration]-long file encoded as
     * JPEGs off the main thread, so its next opening (or a cast) has them at once.
     */
    private fun savePrefetchedFrames(duration: Long) {
        // Frames kept for another duration than the player's: every step is made now, they're of no use
        loadedSidecar = null
        val target = previews
        val last = phonePreviewBucketOf(duration - 1, duration)
        val save = shouldSavePrefetchedPreviews(
            downloaded = target != null,
            loadedFromSidecar = sidecarApplied,
            saved = sidecarSaved,
            everyStepDone = (0..last).all(::isDone),
        )
        if (!save || target == null) return
        sidecarSaved = true
        // What the cache holds now: a frame dropped later stays in the sidecar. A step without one (none in time, an
        // error) is left out, not kept as one without: it may come next time, fetched when it's needed
        val steps = (0..last).mapNotNull { step -> frames[key(step)]?.let { Triple(step, it, frameTimesMs[step]) } }
        val startedAt = SystemClock.elapsedRealtime()
        scope.launch(Dispatchers.IO) {
            val entries = steps.mapNotNull { (step, bitmap, timeMs) ->
                CastPreviewProtocol.encodeWithinBudget { quality ->
                    ByteArrayOutputStream().also { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out) }.toByteArray()
                }?.let { PreviewSidecar.Entry(step, timeMs ?: -1L, it) }
            }
            val first = steps.firstOrNull()?.second ?: return@launch
            val sidecar = PreviewSidecar(
                videoLength = target.video.length(),
                durationMs = duration,
                stepMs = previewBucketSizeMs(duration),
                frameWidth = first.width,
                frameHeight = first.height,
                entries = entries,
            )
            // Frames for this player's duration made meanwhile (after the download, by a cast) stay: another
            // duration's are replaced
            if (target.write(context, sidecar, keeps = { it.matchesDuration(duration) })) {
                Log.i(TAG, "Saved the prefetched preview frames next to the episode in ${SystemClock.elapsedRealtime() - startedAt} ms")
            }
        }
    }

    /** A fetch of [bucket] ended; after a prefetch, the next one comes after a gap. */
    private fun onFetchEnded(bucket: Int) {
        val wasPrefetch = requests.inFlightIsPrefetch
        requests.finished(bucket)
        if (wasPrefetch) {
            schedule.onDecodeEnded(PREVIEW_PREFETCH_GAP_LOCAL_MS)
            if (prefetchEnabled) scheduleTick(PREVIEW_PREFETCH_GAP_LOCAL_MS)
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
        // Both sides within PREVIEW_FRAME_WIDTH_PX, a portrait video's too: what the frames kept next to the episode allow
        val (width, height) = previewFrameSize(videoAspectRatio)
        val bitmap = view.getBitmap(width, height)?.let(::toPreviewFrame) ?: return
        frameBytes = bitmap.allocationByteCount.toLong()
        frames.put(key(bucket), bitmap)
        framesVersion++
        val framePtsMs = releasedFramePtsUs / 1000
        frameTimesMs[bucket] = framePtsMs
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
        framesVersion++
        onFetchEnded(bucket)
        pumpRequests()
    }

    /**
     * [failedPlayer] stopped on [error]: a new player takes the next fetch, the step loading is given up unless the
     * decoder alone failed, and the file after [PREVIEW_MAX_ERRORS] errors.
     */
    private fun onPreviewError(failedPlayer: ExoPlayer, error: PlaybackException) {
        val bucket = requests.inFlight
        val outcome = errors.onError(error.errorCode)
        Log.w(TAG, "Seekbar preview failed on step $bucket, error ${errors.count} of $PREVIEW_MAX_ERRORS on this file", error)
        // Already dropped: a timeout or another file
        if (player !== failedPlayer) return
        handler.removeCallbacks(giveUp)
        failedPlayer.release()
        player = null
        if (bucket != null) {
            if (outcome.markStepUnavailable) unavailable += bucket
            onFetchEnded(bucket)
        }
        if (outcome.failSource) failed = true
        framesVersion++
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
                Log.d(TAG, "Load failed for step ${requests.inFlight}: ${error.javaClass.simpleName}")
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
                // Out of the player's own callback
                handler.post { onPreviewError(previewPlayer, error) }
            }
        })
        surface?.let(previewPlayer::setVideoSurface)
        previewPlayer.setMediaSource(mediaSource)
        player = previewPlayer
        return previewPlayer
    }

    private companion object {
        const val TAG = "SeekPreviewPlayer"
        const val DEFAULT_ASPECT_RATIO = 16f / 9f

        /** A paused screen keeps the decoder this long, as the cast's idle grabber. */
        const val IDLE_RELEASE_MS = 30_000L

        /** How often the prefetch looks at the main player. */
        const val PREFETCH_TICK_MS = 1_000L


        /** Beyond the step, how far before a frame may be. */
        const val FRAME_WINDOW_BEFORE_MS = 1_000L

        /** Beyond the step, how far after: a keyframe interval of a stream seeking by bytes (Kwik's reach 14 s). */
        const val FRAME_WINDOW_AFTER_MS = 15_000L
    }
}
