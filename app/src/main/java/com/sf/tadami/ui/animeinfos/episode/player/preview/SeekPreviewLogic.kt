package com.sf.tadami.ui.animeinfos.episode.player.preview

import android.content.ComponentCallbacks2
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer
import com.sf.tadami.data.download.isLocalFile
import com.sf.tadami.source.model.StreamSource

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

/** Bytes of preview frames kept per stream. */
const val PREVIEW_FRAME_CACHE_BYTES = 12L * 1024 * 1024

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

/** Buffer the main player keeps ahead before preview loads may share the network with it. */
const val MAIN_PLAYER_MIN_BUFFER_AHEAD_MS = 10_000L

/**
 * Whether the main player can spare network for preview loads: it isn't rebuffering, and it has
 * [MAIN_PLAYER_MIN_BUFFER_AHEAD_MS] buffered ahead or the whole video loaded.
 */
fun mainPlayerCanSpareNetwork(
    isBuffering: Boolean,
    bufferedAheadMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
): Boolean {
    if (isBuffering) return false
    return bufferedAheadMs >= MAIN_PLAYER_MIN_BUFFER_AHEAD_MS || (durationMs > 0L && bufferedPositionMs >= durationMs)
}

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

/** Start and end (exclusive) of the preview step [bucket]; the last step ends with the video. */
fun previewBucketRangeMs(bucket: Int, durationMs: Long): LongRange {
    val size = previewBucketSizeMs(durationMs)
    val start = bucket * size
    val end = (start + size).coerceAtMost(durationMs.coerceAtLeast(start + 1))
    return start until end
}

/**
 * Identity of a stream for the preview frames: its URL with its server and quality, or the file of a
 * download. Frames of one identity never change, frames of another are never shown for it.
 */
fun previewSourceIdentity(source: StreamSource): String {
    return if (source.isLocalFile) source.url else "${source.url}|${source.server}|${source.quality}"
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
 * Which preview step to fetch: only the one under the finger, never one cached or already loading, and
 * one at a time. A finger back over the step loading waits for it.
 */
class BucketRequests {
    var inFlight: Int? = null
        private set

    private var target: Int? = null

    /** The finger is over [bucket], or null once the drag ended. */
    fun aim(bucket: Int?) {
        target = bucket
    }

    /** The step to fetch now, marked as loading; null when there's none, it [isDone] already, or one is loading. */
    fun next(isDone: (Int) -> Boolean): Int? {
        if (inFlight != null) return null
        val bucket = target?.takeUnless(isDone) ?: return null
        inFlight = bucket
        return bucket
    }

    /** The fetch of [bucket] ended, its frame cached or given up. */
    fun finished(bucket: Int) {
        if (inFlight == bucket) inFlight = null
    }

    fun clear() {
        inFlight = null
        target = null
    }
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
