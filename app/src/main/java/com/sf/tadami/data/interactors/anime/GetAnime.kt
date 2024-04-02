package com.sf.tadami.data.interactors.anime

import android.util.Log
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.domain.anime.Anime
import kotlinx.coroutines.flow.Flow

class GetAnime(
    private val animeRepository: AnimeRepository,
) {

    suspend fun await(id: Long): Anime? {
        return try {
            animeRepository.getAnimeById(id)
        } catch (e: Exception) {
            Log.e("GetAnime interactor", e.stackTraceToString())
            null
        }
    }

    fun subscribe(id: Long): Flow<Anime> {
        return animeRepository.getAnimeByIdAsFlow(id)
    }

    fun subscribe(url: String, sourceId: Long): Flow<Anime?> {
        return animeRepository.getAnimeByUrlAndSourceIdAsFlow(sourceId,url)
    }
}
