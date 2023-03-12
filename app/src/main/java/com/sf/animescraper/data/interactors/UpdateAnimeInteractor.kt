package com.sf.animescraper.data.interactors

import com.sf.animescraper.data.anime.AnimeRepository
import com.sf.animescraper.data.episode.EpisodeRepository
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.domain.anime.toUpdateAnime
import com.sf.animescraper.domain.episode.Episode
import com.sf.animescraper.domain.episode.copyFromSEpisode
import com.sf.animescraper.domain.episode.toUpdateEpisode
import com.sf.animescraper.network.api.model.SAnime
import com.sf.animescraper.network.api.model.SEpisode

class UpdateAnimeInteractor(
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
) {
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
    ) {
        val sourceEpisodes = remoteEpisodes
            .distinctBy { it.url }
            .mapIndexed { i,sEpisode ->
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

        for (sourceEpisode in sourceEpisodes) {
            val dbEpisode = localEpisodes.find { it.url == sourceEpisode.url }

            if(dbEpisode != null){
                episodesToUpdate.add(dbEpisode.copy(
                    url = sourceEpisode.url,
                    name = sourceEpisode.name,
                    episodeNumber = sourceEpisode.episodeNumber,
                    date = sourceEpisode.date,
                    sourceOrder = sourceEpisode.sourceOrder
                ))
            }else{
                episodesToAdd.add(sourceEpisode)
            }
        }

        if(episodesToDelete.isNotEmpty()){
            val toDeleteIds = episodesToDelete.map { it.id }
            episodeRepository.deleteEpisodesById(toDeleteIds)
        }

        if(episodesToUpdate.isNotEmpty()){
            val toUpdateEpisodes = episodesToUpdate.map { it.toUpdateEpisode() }
            episodeRepository.updateAll(toUpdateEpisodes)
        }

        if(episodesToAdd.isNotEmpty()){
            episodeRepository.addAll(episodesToAdd)
        }
    }
}