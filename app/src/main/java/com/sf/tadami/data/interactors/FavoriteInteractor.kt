package com.sf.tadami.data.interactors

import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.domain.anime.FavoriteAnime
import kotlinx.coroutines.flow.Flow

class FavoriteInteractor(
    private val animeRepository: AnimeRepository
) {

    suspend fun await() : List<FavoriteAnime>{
        return animeRepository.getFavoriteAnimes()
    }

    fun subscribe(): Flow<List<FavoriteAnime>> {
        return animeRepository.getFavoriteAnimesAsFlow()
    }
}