package com.sf.tadami.ui.animeinfos.episode.player

import com.sf.tadami.data.download.isLocalFile
import com.sf.tadami.source.model.StreamSource

/**
 * A downloaded episode is cast from its source's streams (the TV can't read the phone's files).
 * Once the cast ends, the local player goes back to the downloaded files.
 */
object CastFallback {

    sealed interface AfterCast {
        /** Plays downloaded files already, or nothing is played */
        data object Keep : AfterCast

        /** The downloaded files the cast replaced, of this same episode */
        data class Restore(val source: StreamSource) : AfterCast

        /** Another episode than the one the cast started on: its files, when downloaded */
        data object Lookup : AfterCast
    }

    /**
     * What the local player plays once the cast ends, with [episodeId] the current episode, [selected]
     * its stream, and [beforeCast] the downloaded files the cast replaced and their episode id.
     */
    fun afterCast(episodeId: Long?, selected: StreamSource?, beforeCast: Pair<Long, StreamSource>?): AfterCast {
        return when {
            episodeId == null || selected?.isLocalFile == true -> AfterCast.Keep
            beforeCast != null && beforeCast.first == episodeId -> AfterCast.Restore(beforeCast.second)
            else -> AfterCast.Lookup
        }
    }
}
