package com.sf.tadami.ui.animeinfos.episode.player.preview

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer

/** Where [positionMs] sits on a [durationMs]-long timeline, from 0 to 1 (0 while the duration is unknown). */
fun seekFraction(positionMs: Long, durationMs: Long): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/**
 * Left edge of a [previewWidth]-wide preview centred over the slider thumb at [fraction], kept between
 * [boundsStart] and [boundsEnd] (the screen edges). A Material slider moves the thumb centre from
 * `thumbWidth / 2` to `sliderWidth - thumbWidth / 2`. All values are in the same pixel space.
 */
fun seekPreviewLeft(
    fraction: Float,
    sliderStart: Float,
    sliderWidth: Float,
    thumbWidth: Float,
    previewWidth: Float,
    boundsStart: Float,
    boundsEnd: Float,
): Float {
    val thumbCenter = sliderStart + thumbWidth / 2 + fraction.coerceIn(0f, 1f) * (sliderWidth - thumbWidth)
    val maxLeft = boundsEnd - previewWidth
    if (maxLeft < boundsStart) return boundsStart
    return (thumbCenter - previewWidth / 2).coerceIn(boundsStart, maxLeft)
}

/**
 * The preview player's tracks: the lowest-bitrate video only, no audio nor subtitles, so a scrub
 * fetches the smallest segments and decodes the smallest frames.
 */
fun previewTrackSelectionParameters(base: TrackSelectionParameters): TrackSelectionParameters {
    return base.buildUpon()
        .setForceLowestBitrate(true)
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        .build()
}

/** Width of a preview frame: 256×144 for a 16:9 video. */
const val PREVIEW_FRAME_WIDTH_PX = 256

/** Bytes of preview frames kept per stream, for the cast (JPEGs of 3 to 16 KB: well over a thousand). */
const val PREVIEW_FRAME_CACHE_BYTES = 12L * 1024 * 1024

/** Bytes of one phone preview frame: 256×144 in RGB_565, 72 KiB. */
const val PREVIEW_FRAME_BYTES = PREVIEW_FRAME_WIDTH_PX * 144L * 2

/**
 * Bytes of the phone player's preview frames: 341 of [PREVIEW_FRAME_BYTES], every step of a 2 h 50 film
 * (steps are 30 s at most past 50 min: 2.5 h is 300 steps, 21 MiB). Bitmap pixels live in native memory.
 */
const val PREVIEW_BITMAP_CACHE_BYTES = 24L * 1024 * 1024

/** Between two prefetched steps of a downloaded episode: the main player decodes too. */
const val PREVIEW_PREFETCH_GAP_LOCAL_MS = 500L

/**
 * The phone player's steady playback before its prefetch starts: shorter than the cast's [PreviewPrefetch.STABLE_MS],
 * a downloaded episode's prefetch reads no network, so the first scrubs find the preview player ready sooner.
 */
const val PHONE_PREFETCH_STABLE_LOCAL_MS = 1_000L

/**
 * Steps around the playback position the phone player's prefetch makes first, before the coarse to fine grid of
 * the whole timeline ([coarseToFineOrder]): ±2 min of a 24 min episode, past the +85 s intro skip.
 */
const val PHONE_PREFETCH_NEAR_STEPS = 8

/** The phone player's prefetch schedule, on the clock [now]. */
fun phonePreviewPrefetch(now: () -> Long) = PreviewPrefetch(now).apply { stableMs = PHONE_PREFETCH_STABLE_LOCAL_MS }

/**
 * Whether the phone player prefetches its preview frames: both settings on, a [downloaded] episode playing (a
 * streamed one has no preview frames).
 */
fun phonePreviewPrefetchOn(seekPreview: Boolean, prefetch: Boolean, downloaded: Boolean): Boolean =
    seekPreview && prefetch && downloaded

/**
 * Whether the phone player's prefetched frames are saved next to the episode once its prefetch is done: a
 * [downloaded] episode whose frames weren't [loadedFromSidecar] (none kept, or unusable), not [saved] already, and
 * whose every step is done (a frame, or none in time); a cache that filled up first saves nothing.
 */
fun shouldSavePrefetchedPreviews(downloaded: Boolean, loadedFromSidecar: Boolean, saved: Boolean, everyStepDone: Boolean): Boolean =
    downloaded && !loadedFromSidecar && !saved && everyStepDone

/** Errors of the preview player on one file before its preview gives up, until the file changes. */
const val PREVIEW_MAX_ERRORS = 3

/** What an error of the preview player costs: the step it was loading, and maybe the whole file. */
data class PreviewErrorOutcome(val markStepUnavailable: Boolean, val failSource: Boolean)

/**
 * The preview player's errors on the current file. A decoder that couldn't start, or was taken back (by the
 * main player, another app), costs nothing but a new preview player; any other error (a part of the file that
 * can't be read) the step it was loading, as a timeout does. The [PREVIEW_MAX_ERRORS]th error fails the file.
 */
class PreviewErrors {
    var count = 0
        private set

    /** An error of code [errorCode] ([PlaybackException.errorCode]) happened: what it costs. */
    fun onError(errorCode: Int): PreviewErrorOutcome {
        count++
        val decoder = errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
            errorCode == PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED
        return PreviewErrorOutcome(markStepUnavailable = !decoder, failSource = count >= PREVIEW_MAX_ERRORS)
    }

    /** Another file: its errors only count. */
    fun reset() {
        count = 0
    }
}

/** A step whose frame hasn't come after this long gives none, and isn't fetched again for the stream. */
const val PREVIEW_FETCH_TIMEOUT_MS = 10_000L

/** A single video renderer for the preview players: no audio, text nor metadata renderer is even created. */
@OptIn(UnstableApi::class)
fun previewRenderersFactory(context: Context) = RenderersFactory { handler, videoListener, _, _, _ ->
    arrayOf(
        MediaCodecVideoRenderer.Builder(context)
            .setEventHandler(handler)
            .setEventListener(videoListener)
            .setEnableDecoderFallback(true)
            .build()
    )
}

/** The preview players' buffering: about one segment around the target, nothing more, nothing kept behind. */
@OptIn(UnstableApi::class)
fun previewLoadControl(): DefaultLoadControl = DefaultLoadControl.Builder()
    .setBufferDurationsMs(PREVIEW_MIN_BUFFER_MS, PREVIEW_MAX_BUFFER_MS, PREVIEW_MIN_BUFFER_MS, PREVIEW_MIN_BUFFER_MS)
    .setTargetBufferBytes(PREVIEW_TARGET_BUFFER_BYTES)
    .setPrioritizeTimeOverSizeThresholds(false)
    .setBackBuffer(0, false)
    .build()

private const val PREVIEW_MIN_BUFFER_MS = 500
private const val PREVIEW_MAX_BUFFER_MS = 1_000
private const val PREVIEW_TARGET_BUFFER_BYTES = 4 * 1024 * 1024

/** Preview steps: 1% of the duration, so about 100 frames per video, but 5 s at least and 30 s at most. */
const val PREVIEW_BUCKET_MIN_MS = 5_000L
const val PREVIEW_BUCKET_MAX_MS = 30_000L

/** Length of a preview step of a [durationMs]-long video. */
fun previewBucketSizeMs(durationMs: Long): Long = (durationMs / 100).coerceIn(PREVIEW_BUCKET_MIN_MS, PREVIEW_BUCKET_MAX_MS)

/** The preview step [positionMs] falls in; positions out of the video belong to its first or last step. */
fun previewBucketOf(positionMs: Long, durationMs: Long): Int {
    val clamped = positionMs.coerceIn(0L, (durationMs - 1).coerceAtLeast(0L))
    return (clamped / previewBucketSizeMs(durationMs)).toInt()
}

/**
 * The phone player's preview step of [positionMs]: [previewBucketOf], but a last step shorter than half a step
 * belongs to the one before it. The step size is rounded down, so the last step is often a leftover of a few ms
 * (26 ms of a 1,430,026 ms episode), too short for a seek to render a frame in it.
 */
fun phonePreviewBucketOf(positionMs: Long, durationMs: Long): Int {
    val bucket = previewBucketOf(positionMs, durationMs)
    if (durationMs <= 0L) return bucket
    val last = previewBucketOf(durationMs - 1, durationMs)
    val size = previewBucketSizeMs(durationMs)
    return if (bucket == last && last > 0 && durationMs - last * size < size / 2) last - 1 else bucket
}

/**
 * The step whose frame stands for [bucket]: itself when it [has] one, else the nearest within [maxSteps] steps
 * between 0 and [last], the earlier on a tie; null when none has.
 */
fun nearestPreviewBucket(bucket: Int, last: Int, maxSteps: Int, has: (Int) -> Boolean): Int? {
    if (has(bucket)) return bucket
    for (distance in 1..maxSteps) {
        val before = bucket - distance
        if (before >= 0 && has(before)) return before
        val after = bucket + distance
        if (after <= last && has(after)) return after
    }
    return null
}

/** How far a stand-in frame may be from its step: 4 steps and 60 s at most, 1 step at least. */
fun previewNearestMaxSteps(durationMs: Long): Int =
    (PREVIEW_NEAREST_MAX_MS / previewBucketSizeMs(durationMs)).toInt().coerceIn(1, PREVIEW_NEAREST_MAX_STEPS)

private const val PREVIEW_NEAREST_MAX_MS = 60_000L
private const val PREVIEW_NEAREST_MAX_STEPS = 4

/** Start and end (exclusive) of the preview step [bucket]; the last step ends with the video. */
fun previewBucketRangeMs(bucket: Int, durationMs: Long): LongRange {
    val size = previewBucketSizeMs(durationMs)
    val start = bucket * size
    val end = (start + size).coerceAtMost(durationMs.coerceAtLeast(start + 1))
    return start until end
}

/** A preview frame: the step [bucket] of the stream [sourceId]. */
data class PreviewFrameKey(val sourceId: String, val bucket: Int)

/**
 * Least recently used preview frames of the current source, bounded in bytes. A new source drops all the
 * frames; frames of another source than the current one are refused.
 */
class PreviewFrameCache<V : Any>(private val maxBytes: Long, private val sizeOf: (V) -> Long) {
    private val entries = LinkedHashMap<PreviewFrameKey, V>(16, 0.75f, true)

    var sourceId: String? = null
        private set

    var bytes = 0L
        private set

    val size: Int
        get() = entries.size

    operator fun get(key: PreviewFrameKey): V? = entries[key]

    /** Whether the frame is cached, without making it recently used. */
    operator fun contains(key: PreviewFrameKey): Boolean = entries.containsKey(key)

    /** Whether a frame of [frameBytes] fits without dropping another. */
    fun hasRoomFor(frameBytes: Long): Boolean = bytes + frameBytes <= maxBytes

    fun put(key: PreviewFrameKey, value: V) {
        if (key.sourceId != sourceId) return
        entries.put(key, value)?.let { bytes -= sizeOf(it) }
        bytes += sizeOf(value)
        val iterator = entries.entries.iterator()
        while (bytes > maxBytes && iterator.hasNext()) {
            bytes -= sizeOf(iterator.next().value)
            iterator.remove()
        }
    }

    /** Makes [id] the current source; another source than the current one drops every frame. */
    fun setSource(id: String?) {
        if (id == sourceId) return
        clear()
        sourceId = id
    }

    fun clear() {
        entries.clear()
        bytes = 0L
    }
}

/**
 * Which preview step to fetch: the one under the finger, never one cached or already loading, and one at
 * a time. A finger back over the step loading waits for it. Without a finger on the seekbar, the
 * background prefetch may fetch a step; the finger's step then takes over, unless it is the one loading.
 */
class BucketRequests {
    var inFlight: Int? = null
        private set

    /** The fetch in flight is a prefetch, not the finger's step. */
    var inFlightIsPrefetch = false
        private set

    private var target: Int? = null

    /** A finger is on the seekbar: no prefetch starts. */
    val scrubbing: Boolean
        get() = target != null

    /** The finger is over [bucket], or null once the drag ended. */
    fun aim(bucket: Int?) {
        target = bucket
    }

    /**
     * The step to fetch now, marked as loading; null when there's none, it [isDone] already, or one is
     * loading — but a prefetch of another step, which the finger's step takes over.
     */
    fun next(isDone: (Int) -> Boolean): Int? {
        val bucket = target?.takeUnless(isDone) ?: return null
        val loading = inFlight
        if (loading != null && (!inFlightIsPrefetch || loading == bucket)) return null
        inFlight = bucket
        inFlightIsPrefetch = false
        return bucket
    }

    /** Marks the prefetch of [bucket] as loading: false when a fetch loads or a finger is on the seekbar. */
    fun prefetch(bucket: Int): Boolean {
        if (inFlight != null || target != null) return false
        inFlight = bucket
        inFlightIsPrefetch = true
        return true
    }

    /** The fetch of [bucket] ended, its frame cached or given up. */
    fun finished(bucket: Int) {
        if (inFlight == bucket) {
            inFlight = null
            inFlightIsPrefetch = false
        }
    }

    fun clear() {
        inFlight = null
        inFlightIsPrefetch = false
        target = null
    }
}

/** What the phone player's background prefetch does now. */
sealed interface PreviewPrefetchStep {
    /** Fetch the frame of [bucket]. */
    data class Fetch(val bucket: Int) : PreviewPrefetchStep

    /** Nothing now: the playback isn't steady, the gap isn't over, a fetch loads or a finger scrubs. */
    data object Wait : PreviewPrefetchStep

    /** Nothing more: every step is cached or gives no frame, or the cache is full (nothing is evicted for it). */
    data object Done : PreviewPrefetchStep
}

/**
 * The prefetch's next move in a [durationMs]-long stream, from the playback [schedule], what [requests]
 * load, the steps [isDone] and whether the cache [hasRoom] for one more frame.
 */
fun nextPreviewPrefetch(
    schedule: PreviewPrefetch,
    requests: BucketRequests,
    durationMs: Long,
    isDone: (Int) -> Boolean,
    hasRoom: Boolean,
): PreviewPrefetchStep {
    if (!schedule.runnable() || !schedule.ready() || requests.inFlight != null || requests.scrubbing) {
        return PreviewPrefetchStep.Wait
    }
    // A downloaded episode's: the whole timeline soon has a frame near each position, see previewNearestMaxSteps
    val bucket = schedule.nextStep(
        durationMs,
        last = phonePreviewBucketOf(durationMs - 1, durationMs),
        order = { center, last -> coarseToFineOrder(center, last, PHONE_PREFETCH_NEAR_STEPS) },
        isDone = isDone,
    ) ?: return PreviewPrefetchStep.Done
    return if (hasRoom) PreviewPrefetchStep.Fetch(bucket) else PreviewPrefetchStep.Done
}

/**
 * [argb] as a preview frame: RGB_565, half the memory, dithered so gradients don't band. [argb] is
 * recycled.
 */
fun toPreviewFrame(argb: Bitmap): Bitmap {
    val frame = Bitmap.createBitmap(argb.width, argb.height, Bitmap.Config.RGB_565)
    Canvas(frame).drawBitmap(argb, 0f, 0f, Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG))
    argb.recycle()
    return frame
}

/**
 * Whether a memory trim drops the preview frames: on memory pressure, or once the app is a background
 * process. Hiding the UI alone (pause, PiP, home) keeps them.
 */
@Suppress("DEPRECATION")
fun shouldDropPreviewFrames(trimLevel: Int): Boolean {
    return trimLevel == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
        trimLevel == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
        trimLevel >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
}
