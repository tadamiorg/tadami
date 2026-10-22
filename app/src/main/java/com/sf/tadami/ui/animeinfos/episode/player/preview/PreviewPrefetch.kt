package com.sf.tadami.ui.animeinfos.episode.player.preview

/**
 * When and which preview step is prefetched in the background, from the status of the player that
 * plays the stream (the phone's own, or the TV's when casting): only once it has been PLAYING steadily
 * for [STABLE_MS], never while it buffers or loads, and not after [LONG_PAUSE_MS] of pause (battery,
 * data). Steps go nearest the playback position first, then outward, re-centred on every pick (so
 * after a seek too); one decode at a time with a gap after each. Pure: the clock is given.
 */
class PreviewPrefetch(private val now: () -> Long) {

    /** The playing player's state. */
    enum class Playback { PLAYING, PAUSED, BUFFERING, LOADING, IDLE }

    /** The playback position, as last reported. */
    var positionMs = 0L
        private set

    private var playingSince: Long? = null
    private var pausedSince: Long? = null
    private var stable = false
    private var nextAt = 0L

    /** Another stream: steady playback is awaited again. */
    fun reset() {
        playingSince = null
        pausedSince = null
        stable = false
        nextAt = 0L
    }

    /** The player's status: [playback] (null when unknown) at [positionMs]. */
    fun onStatus(playback: Playback?, positionMs: Long) {
        val time = now()
        this.positionMs = positionMs.coerceAtLeast(0L)
        when (playback) {
            Playback.PLAYING -> {
                val since = playingSince ?: time.also { playingSince = it }
                pausedSince = null
                if (time - since >= STABLE_MS) stable = true
            }
            // A pause keeps the steadiness: the prefetch goes on, for a while
            Playback.PAUSED -> {
                playingSince = null
                if (pausedSince == null) pausedSince = time
            }
            else -> {
                playingSince = null
                pausedSince = null
                stable = false
            }
        }
    }

    /** The playback allows a prefetch now: steady, not buffering nor loading, not paused for long. */
    fun runnable(): Boolean {
        if (!stable) return false
        val paused = pausedSince ?: return true
        return now() - paused < LONG_PAUSE_MS
    }

    /** The gap after the last prefetch decode is over. */
    fun ready(): Boolean = now() >= nextAt

    /** A prefetch decode ended: the next one waits [gapMs]. */
    fun onDecodeEnded(gapMs: Long) {
        nextAt = now() + gapMs
    }

    /** The step to prefetch now in a [durationMs]-long stream: the nearest the position not [isDone]; null when all are. */
    fun nextStep(durationMs: Long, isDone: (Int) -> Boolean): Int? {
        if (durationMs <= 0L) return null
        val last = previewBucketOf(durationMs - 1, durationMs)
        val center = previewBucketOf(positionMs, durationMs)
        return prefetchOrder(center, last).firstOrNull { !isDone(it) }
    }

    companion object {
        /** Playing this long without buffering before the prefetch starts (or starts again). */
        const val STABLE_MS = 5_000L

        /** A pause longer than this stops the prefetch until playback resumes. */
        const val LONG_PAUSE_MS = 10 * 60_000L
    }
}

/** Steps from [center] outward within 0..[last]: center, center+1, center-1, center+2, … */
fun prefetchOrder(center: Int, last: Int): Sequence<Int> {
    if (last < 0) return emptySequence()
    val start = center.coerceIn(0, last)
    return sequence {
        yield(start)
        var distance = 1
        while (start + distance <= last || start - distance >= 0) {
            if (start + distance <= last) yield(start + distance)
            if (start - distance >= 0) yield(start - distance)
            distance++
        }
    }
}
