package com.sf.animescraper.data.interactors

import com.sf.animescraper.data.anime.AnimeRepository
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.domain.anime.toUpdateAnime
import com.sf.animescraper.network.api.model.SAnime

class UpdateAnimeInteractor(
    private val animeRepository: AnimeRepository,
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
}