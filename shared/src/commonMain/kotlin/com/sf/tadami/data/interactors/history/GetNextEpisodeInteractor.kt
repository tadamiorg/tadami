package com.sf.tadami.data.interactors.history

import com.sf.tadami.data.history.HistoryRepository
import com.sf.tadami.data.interactors.anime.AnimeWithEpisodesInteractor
import com.sf.tadami.domain.episode.Episode

class GetNextEpisodeInteractor(
    private val animeWithEpisodesInteractor: AnimeWithEpisodesInteractor,
    private val historyRepository: HistoryRepository,
) {

    suspend fun await(onlyUnseen: Boolean = true): List<Episode> {
        val history = historyRepository.getLastHistory() ?: return emptyList()
        return await(history.animeId, history.episodeId, onlyUnseen)
    }

    suspend fun await(animeId: Long, onlyUnseen: Boolean = true): List<Episode> {
        val episodes = animeWithEpisodesInteractor.awaitEpisodes(animeId)
            .sortedByDescending {
                it.sourceOrder
            }

        return if (onlyUnseen) {
            episodes.filterNot { it.seen }
        } else {
            episodes
        }
    }

    suspend fun await(animeId: Long, fromEpisodeId: Long, onlyUnseen: Boolean = true): List<Episode> {
        val episodes = await(animeId, onlyUnseen)
        val currEpisodeIndex = episodes.indexOfFirst { it.id == fromEpisodeId }.coerceAtLeast(0)
        val nextEpisodes = episodes.subList(currEpisodeIndex, episodes.size)

        if (onlyUnseen) {
            return nextEpisodes
        }
        // The "next episode" is either:
        // - The current episode if it isn't completely seen
        // - The episodes after the current episode if the current one is completely seen
        val fromEpisode = episodes.getOrNull(currEpisodeIndex)
        return if (fromEpisode != null && !fromEpisode.seen) {
            nextEpisodes
        } else {
            nextEpisodes.drop(1)
        }
    }
}