package com.sf.tadami.ui.animeinfos.episode.cast.preview

import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewPrefetch

/** Between two prefetch decodes of a downloaded episode, read from the phone's storage. */
const val CAST_PREFETCH_GAP_LOCAL_MS = 100L

/** What the prefetch does at a tick. */
sealed interface PrefetchDecision {
    /** Decode this step now. */
    class Start(val fetch: CastPreviewFrames.Action.Fetch) : PrefetchDecision

    /** Nothing now: not [allowed][nextPrefetch], the playback isn't steady, a decode runs, or the gap isn't over. */
    data object Wait : PrefetchDecision

    /** Every step of the stream has its frame, is known to have none, or the receiver holds it. */
    data object Complete : PrefetchDecision
}

/**
 * The prefetch's next move for [source]: [allowed] by the settings (prefetch setting on, and the
 * preview's own rules), when [prefetch]'s playback gating lets it and nothing decodes in [frames].
 * The steps [receiverHas] count as done.
 */
fun nextPrefetch(
    source: CastPreviewSource,
    prefetch: PreviewPrefetch,
    frames: CastPreviewFrames,
    allowed: Boolean,
    receiverHas: Set<Int>,
): PrefetchDecision {
    if (!allowed || !prefetch.runnable() || !prefetch.ready() || frames.inFlight != null) return PrefetchDecision.Wait
    val step = prefetch.nextStep(source.duration) { frames.isDone(it) || it in receiverHas }
        ?: return PrefetchDecision.Complete
    return frames.prefetch(source.requestFor(step))?.let(PrefetchDecision::Start) ?: PrefetchDecision.Wait
}
