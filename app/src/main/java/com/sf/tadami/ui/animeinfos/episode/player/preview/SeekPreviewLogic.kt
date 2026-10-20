package com.sf.tadami.ui.animeinfos.episode.player.preview

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters

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

/**
 * Throttle where only the newest value matters: a value goes through at most once per [intervalMs],
 * a value pushed sooner waits and is replaced by any newer one (the stale ones are never applied).
 */
class LatestThrottle(private val intervalMs: Long) {
    private var pending: Long? = null
    private var lastEmitAtMs: Long? = null

    /** A value waits for its turn. */
    val hasPending: Boolean
        get() = pending != null

    /** Replaces the waiting value with [value]; [poll] tells when it can be applied. */
    fun push(value: Long) {
        pending = value
    }

    /** The waiting value, when its turn has come at [nowMs]; else null. */
    fun poll(nowMs: Long): Long? {
        val value = pending ?: return null
        if (delayMs(nowMs) > 0L) return null
        pending = null
        lastEmitAtMs = nowMs
        return value
    }

    /** How long the waiting value still has to wait at [nowMs]: 0 when it can go, or when none waits. */
    fun delayMs(nowMs: Long): Long {
        if (pending == null) return 0L
        val last = lastEmitAtMs ?: return 0L
        return (last + intervalMs - nowMs).coerceAtLeast(0L)
    }

    /** Drops the waiting value and the timing: the next offer goes through at once. */
    fun clear() {
        pending = null
        lastEmitAtMs = null
    }
}
