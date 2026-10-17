package com.sf.tadami.ui.animeinfos.episode.player

import com.sf.tadami.data.download.isLocalFile
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.ui.animeinfos.episode.cast.proxy.isLocalMediaUrl

/**
 * A downloaded episode is cast from the phone (LocalCast), else from its source's streams. Once the
 * cast ends, the local player goes back to the downloaded files.
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

    /**
     * Whether the downloaded files [kept] play again after the cast: the video, its separate audio and
     * its subtitles are all still there, [exists] telling a file is. One deleted during the cast (after
     * watching, by hand, all the downloads of the anime) and the stream goes on.
     */
    fun canRestore(kept: StreamSource, exists: (url: String) -> Boolean): Boolean {
        val files = listOf(kept.url) + kept.audioTracks.map { it.url } + kept.subtitleTracks.map { it.url }
        return files.all(exists)
    }

    /**
     * The episode has no downloaded files after the cast ([AfterCast.Lookup]): whether [selected], the
     * stream the cast played, is fetched from the source again. Downloaded files the phone served stop
     * with the cast (deleted during it); a stream of the source goes on.
     */
    fun needsOnlineStream(selected: StreamSource?): Boolean = selected != null && isLocalMediaUrl(selected.url)
}
