package com.sf.tadami.data.interactors.anime

import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AnimeWithEpisodesInteractor(
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
) {

    fun subscribe(id: Long): Flow<Pair<Anime, List<Episode>>> {
        return combine(
            animeRepository.getAnimeByIdAsFlow(id),
            episodeRepository.getEpisodesByAnimeIdAsFlow(id),
        ) { anime, episodes ->
            Pair(anime, episodes)
        }
    }

    suspend fun awaitAnime(id: Long): Anime {
        return animeRepository.getAnimeById(id)
    }

    suspend fun awaitEpisodes(id: Long): List<Episode> {
        return episodeRepository.getEpisodesByAnimeId(id)
    }
}