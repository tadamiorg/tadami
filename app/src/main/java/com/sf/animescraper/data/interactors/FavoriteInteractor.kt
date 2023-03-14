package com.sf.animescraper.data.interactors

import com.sf.animescraper.data.anime.AnimeRepository
import com.sf.animescraper.data.episode.EpisodeRepository
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.domain.anime.FavoriteAnime
import com.sf.animescraper.domain.episode.Episode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class FavoriteInteractor(
    private val animeRepository: AnimeRepository
) {

    fun subscribe(): Flow<List<FavoriteAnime>> {
        return animeRepository.getFavoriteAnimes()
    }
}