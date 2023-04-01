package com.sf.tadami.data.interactors

import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.UpdateAnime
import com.sf.tadami.domain.anime.toUpdateAnime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.domain.episode.UpdateEpisode
import com.sf.tadami.domain.episode.copyFromSEpisode
import com.sf.tadami.domain.episode.toUpdateEpisode
import com.sf.tadami.network.api.model.SAnime
import com.sf.tadami.network.api.model.SEpisode
import java.util.*

class UpdateAnimeInteractor(
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
) {

    suspend fun updateLibraryAnime(
        anime: Anime,
        favorite: Boolean
    ) {
        animeRepository.updateAnime(UpdateAnime(id = anime.id, favorite = favorite))
    }

    suspend fun updateLibrary(
        libraryAnimeIds: Set<Long>,
        favorite: Boolean
    ) {
        animeRepository.updateAll(libraryAnimeIds.map { UpdateAnime(id = it, favorite = favorite) })
    }

    suspend fun awaitUpdateFromSource(
        localAnime: Anime,
        remoteAnime: SAnime,
    ): Boolean {
        val remoteTitle = try {
            remoteAnime.title
        } catch (_: UninitializedPropertyAccessException) {
            ""
        }

        // if the manga isn't a favorite, set its title from source and update in db
        val title = if (remoteTitle.isEmpty() || localAnime.favorite) null else remoteTitle
        val thumbnailUrl = remoteAnime.thumbnailUrl?.takeIf { it.isNotEmpty() }

        return animeRepository.updateAnime(
            localAnime.copyFrom(remoteAnime).toUpdateAnime().copy(
                title = title,
                thumbnailUrl = thumbnailUrl,
                initialized = true
            ),
        )
    }

    suspend fun awaitEpisodesSyncFromSource(
        anime: Anime,
        remoteEpisodes: List<SEpisode>
    ): List<Episode> {
        val sourceEpisodes = remoteEpisodes
            .distinctBy { it.url }
            .mapIndexed { i, sEpisode ->
                Episode.create()
                    .copyFromSEpisode(sEpisode)
                    .copy(animeId = anime.id, sourceOrder = i.toLong())
            }

        val localEpisodes = episodeRepository.getEpisodesByAnimeId(anime.id)

        val episodesToAdd = mutableListOf<Episode>()

        val episodesToUpdate = mutableListOf<Episode>()

        val episodesToDelete = localEpisodes.filterNot { dbEpisode ->
            sourceEpisodes.any {
                dbEpisode.url == it.url
            }
        }

        val rightNow = Date().time

        for (sourceEpisode in sourceEpisodes) {
            val dbEpisode = localEpisodes.find { it.url == sourceEpisode.url }

            if (dbEpisode == null) {
                val toAddEpisode = if (sourceEpisode.dateUpload == 0L) {
                    sourceEpisode.copy(dateFetch = rightNow)
                } else {
                    sourceEpisode.copy(dateFetch = rightNow, dateUpload = sourceEpisode.dateUpload)
                }
                episodesToAdd.add(toAddEpisode)
            } else {
                var toUpdateEpisode = dbEpisode.copy(
                    url = sourceEpisode.url,
                    name = sourceEpisode.name,
                    episodeNumber = sourceEpisode.episodeNumber,
                    sourceOrder = sourceEpisode.sourceOrder
                )

                if (sourceEpisode.dateUpload != 0L) {
                    toUpdateEpisode = toUpdateEpisode.copy(dateUpload = sourceEpisode.dateUpload)
                }
                episodesToUpdate.add(toUpdateEpisode)
            }
        }

        if (episodesToDelete.isNotEmpty()) {
            val toDeleteIds = episodesToDelete.map { it.id }
            episodeRepository.deleteEpisodesById(toDeleteIds)
        }

        if (episodesToUpdate.isNotEmpty()) {
            val toUpdateEpisodes = episodesToUpdate.map { it.toUpdateEpisode() }
            episodeRepository.updateAll(toUpdateEpisodes)
        }

        if (episodesToAdd.isNotEmpty()) {
            episodeRepository.addAll(episodesToAdd)
        }

        return episodesToAdd
    }

    suspend fun awaitSeenEpisodeTimeUpdate(
        episode: Episode,
        totalTime: Long? = null,
        timeSeen: Long? = null
    ) {
        episodeRepository.update(
            UpdateEpisode(
                id = episode.id,
                totalTime = totalTime,
                timeSeen = timeSeen
            )
        )
    }

    suspend fun awaitSeenEpisodeUpdate(
        episodesIds: Set<Long>,
        seen: Boolean
    ) {
        episodeRepository.updateAll(episodesIds.map {
            UpdateEpisode(
                id = it,
                seen = seen,
                timeSeen = 0
            )
        })
    }

    suspend fun awaitSeenAnimeUpdate(
        animeIds: Set<Long>,
        seen: Boolean
    ) {
        episodeRepository.updateAllByAnimeIds(animeIds.map {
            it to UpdateEpisode(
                id = 0,
                seen = seen,
                timeSeen = 0
            )
        })
    }

}