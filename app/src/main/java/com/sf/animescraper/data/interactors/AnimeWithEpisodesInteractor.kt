package com.sf.animescraper.data.interactors

import com.sf.animescraper.data.anime.AnimeRepository
import com.sf.animescraper.data.episode.EpisodeRepository
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.domain.episode.Episode
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

    suspend fun awaitBoth(id: Long): Pair<Anime, List<Episode>>{
        val anime = animeRepository.getAnimeById(id)
        val episodes = episodeRepository.getEpisodesByAnimeId(id)
        return Pair(anime,episodes)
    }

    suspend fun awaitAnime(id: Long): Anime {
        return animeRepository.getAnimeById(id)
    }

    suspend fun awaitEpisodes(id: Long): List<Episode> {
        return episodeRepository.getEpisodesByAnimeId(id)
    }
}