package com.sf.animescraper.data.episode

import com.sf.animescraper.data.DataBaseHandler
import com.sf.animescraper.domain.episode.Episode
import com.sf.animescraper.domain.episode.UpdateEpisode
import kotlinx.coroutines.flow.Flow

interface EpisodeRepository {
    suspend fun addAll(episodes: List<Episode>): List<Episode>
    suspend fun getEpisodesByAnimeId(animeId: Long): List<Episode>
    suspend fun getEpisodeById(episodeId: Long): Episode
    fun getEpisodesByAnimeIdAsFlow(animeId: Long): Flow<List<Episode>>
    suspend fun deleteEpisodesById(ids: List<Long>)
    suspend fun updateAll(episodes: List<UpdateEpisode>)
    suspend fun update(episodeUpdate: UpdateEpisode)

}

class EpisodeRepositoryImpl(
    private val handler: DataBaseHandler
) : EpisodeRepository {
    override suspend fun addAll(episodes: List<Episode>): List<Episode> {
        return try {
            handler.await {
                episodes.map { episode ->
                    episodeQueries.insert(
                        animeId = episode.animeId,
                        url = episode.url,
                        name = episode.name,
                        episodeNumber = episode.episodeNumber.toDouble(),
                        timeSeen = episode.timeSeen,
                        totalTime = episode.totalTime,
                        dateFetch = episode.dateFetch,
                        dateUpload = episode.dateUpload,
                        seen = episode.seen,
                        sourceOrder = episode.sourceOrder
                    )
                    val insertedEpId = episodeQueries.selectLastInsertedRowId().executeAsOne()
                    episode.copy(id = insertedEpId)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }


    }

    override suspend fun getEpisodesByAnimeId(animeId: Long): List<Episode> {
        return try {
            handler.awaitList {
                episodeQueries.getEpisodesByAnimeId(animeId, episodeMapper)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getEpisodeById(episodeId: Long): Episode {
        return handler.awaitOne { episodeQueries.getEpisodeById(episodeId, episodeMapper) }
    }

    override fun getEpisodesByAnimeIdAsFlow(animeId: Long): Flow<List<Episode>> {
        return handler.subscribeToList {
            episodeQueries.getEpisodesByAnimeId(
                animeId,
                episodeMapper
            )
        }
    }

    override suspend fun deleteEpisodesById(ids: List<Long>) {
        handler.await { episodeQueries.delete(ids) }
    }

    override suspend fun updateAll(episodes: List<UpdateEpisode>) {
        handler.await {
            episodes.forEach { episodeUpdate ->
                episodeQueries.update(
                    animeId = episodeUpdate.animeId,
                    url = episodeUpdate.url,
                    name = episodeUpdate.name,
                    episodeNumber = episodeUpdate.episodeNumber?.toDouble(),
                    timeSeen = episodeUpdate.timeSeen,
                    totalTime = episodeUpdate.totalTime,
                    dateFetch = episodeUpdate.dateFetch,
                    dateUpload = episodeUpdate.dateUpload,
                    seen = episodeUpdate.seen,
                    sourceOrder = episodeUpdate.sourceOrder,
                    episodeId = episodeUpdate.id
                )
            }
        }
    }

    override suspend fun update(episodeUpdate: UpdateEpisode) {
        handler.await {
            episodeQueries.update(
                animeId = episodeUpdate.animeId,
                url = episodeUpdate.url,
                name = episodeUpdate.name,
                episodeNumber = episodeUpdate.episodeNumber?.toDouble(),
                timeSeen = episodeUpdate.timeSeen,
                totalTime = episodeUpdate.totalTime,
                dateFetch = episodeUpdate.dateFetch,
                dateUpload = episodeUpdate.dateUpload,
                seen = episodeUpdate.seen,
                sourceOrder = episodeUpdate.sourceOrder,
                episodeId = episodeUpdate.id
            )
        }
    }
}