package com.sf.tadami.data.interactors.library

import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.domain.anime.LibraryAnime
import kotlinx.coroutines.flow.Flow

class LibraryInteractor(
    private val animeRepository: AnimeRepository
) {

    suspend fun await() : List<LibraryAnime>{
        return animeRepository.getLibraryAnimes()
    }

    fun subscribe(): Flow<List<LibraryAnime>> {
        return animeRepository.getLibraryAnimesAsFlow()
    }
}