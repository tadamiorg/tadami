package com.sf.tadami.data.interactors.anime

import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.UpdateAnime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.domain.episode.UpdateEpisode
import com.sf.tadami.domain.episode.copyFromSEpisode
import com.sf.tadami.source.Source
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import java.time.Instant
import java.time.ZonedDateTime

class UpdateAnimeInteractor(
    private val animeRepository: AnimeRepository,
    private val episodeRepository: EpisodeRepository,
    private val fetchInterval : FetchIntervalInteractor
) {

    suspend fun updateLibraryAnime(
        anime: Anime,
        favorite: Boolean
    ) {
        val dateAdded = when (favorite) {
            true -> Instant.now().toEpochMilli()
            false -> 0
        }
        animeRepository.updateAnime(UpdateAnime(id = anime.id, favorite = favorite, dateAdded = dateAdded))
    }

    suspend fun updateLibrary(
        libraryAnimeIds: Set<Long>,
        favorite: Boolean
    ) {
        animeRepository.updateAll(libraryAnimeIds.map { UpdateAnime(id = it, favorite = favorite) })
    }

    suspend fun updateAnimeEpisodeFlags(
        anime: Anime,
        episodeFlags: Long
    ) {
        animeRepository.updateAnime(UpdateAnime(id = anime.id, episodeFlags = episodeFlags))
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

        // if the anime isn't a favorite, set its title from source and update in db
        val title = remoteTitle.ifEmpty { null }
        val thumbnailUrl = remoteAnime.thumbnailUrl?.takeIf { it.isNotEmpty() }

        return animeRepository.updateAnime(
            UpdateAnime(
                id = localAnime.id,
                title = title,
                rawTitle = remoteAnime.rawTitle,
                language = remoteAnime.language,
                description = remoteAnime.description,
                genres = remoteAnime.genres,
                thumbnailUrl = thumbnailUrl,
                release = remoteAnime.release,
                studio = remoteAnime.studio,
                author = remoteAnime.author,
                status = remoteAnime.status,
                initialized = true,
            )
        )
    }

    suspend fun awaitUpdateFetchInterval(
        anime: Anime,
        dateTime: ZonedDateTime = ZonedDateTime.now(),
        window: Pair<Long, Long> = fetchInterval.getWindow(dateTime),
    ): Boolean {
        return fetchInterval.toAnimeUpdateOrNull(anime, dateTime, window)
            ?.let { animeRepository.updateAnime(it)}
            ?: false
    }

    suspend fun awaitEpisodesSyncFromSource(
        anime: Anime,
        remoteEpisodes: List<SEpisode>,
        source : Source,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): List<Episode> {
        val now = ZonedDateTime.now()
        val nowMillis = now.toInstant().toEpochMilli()

        val sourceEpisodes = remoteEpisodes
            .distinctBy { it.url }
            .mapIndexed { i, sEpisode ->
                Episode.create()
                    .copyFromSEpisode(sEpisode)
                    .copy(name = sEpisode.name)
                    .copy(animeId = anime.id, sourceOrder = i.toLong())
            }

        val dbEpisodes = episodeRepository.getEpisodesByAnimeId(anime.id)

        val newEpisodes = mutableListOf<Episode>()
        val updatedEpisodes = mutableListOf<Episode>()
        val removedEpisodes = dbEpisodes.filterNot { dbEpisode ->
            sourceEpisodes.any { sourceEpisode ->
                dbEpisode.url == sourceEpisode.url
            }
        }.toMutableList()

        // Used to not set upload date of older episodes
        // to a higher value than newer episodes
        var maxSeenUploadDate = 0L

        for (sourceEpisode in sourceEpisodes) {
            var episode = sourceEpisode

            // Recognize episode number for the episode.
            val episodeNumber = episode.episodeNumber
            episode = episode.copy(episodeNumber = episodeNumber)

            // An episode the source moved to another url is matched by number to a removed one,
            // updated in place to keep its seen state, progress and history
            val dbEpisode = dbEpisodes.find { it.url == episode.url }
                ?: removedEpisodes.find { it.episodeNumber == episode.episodeNumber }
                    ?.also { removedEpisodes.remove(it) }

            if (dbEpisode == null) {
                val toAddEpisode = if (episode.dateUpload == 0L) {
                    val altDateUpload = if (maxSeenUploadDate == 0L) nowMillis else maxSeenUploadDate
                    episode.copy(dateUpload = altDateUpload)
                } else {
                    maxSeenUploadDate =
                        java.lang.Long.max(maxSeenUploadDate, sourceEpisode.dateUpload)
                    episode
                }
                newEpisodes.add(toAddEpisode)
            } else {
                    var toChangeEpisode = dbEpisode.copy(
                        url = episode.url,
                        name = episode.name,
                        episodeNumber = episode.episodeNumber,
                        languages = episode.languages,
                        sourceOrder = episode.sourceOrder,
                    )
                    if (episode.dateUpload != 0L) {
                        toChangeEpisode = toChangeEpisode.copy(dateUpload = episode.dateUpload)
                    }
                    // Only the episodes the source changed, else every refresh bumps the last update
                    if (toChangeEpisode != dbEpisode) {
                        updatedEpisodes.add(toChangeEpisode)
                    }

            }
        }

        // Return if there's nothing to add, delete, or update to avoid unnecessary db transactions.
        if (newEpisodes.isEmpty() && removedEpisodes.isEmpty() && updatedEpisodes.isEmpty()) {
            if (manualFetch || anime.fetchInterval == 0 || anime.nextUpdate < fetchWindow.first) {
                awaitUpdateFetchInterval(
                    anime,
                    now,
                    fetchWindow,
                )
            }
            return emptyList()
        }

        // Date fetch is set in such a way that the upper ones will have bigger value than the lower ones
        // Sources MUST return the episodes from most to less recent, which is common.
        var itemCount = newEpisodes.size
        val toAdd = newEpisodes.map { it.copy(dateFetch = nowMillis + itemCount--) }

        // Only the fields from the source: the seen state and progress read above may be stale by now
        val episodeUpdates = updatedEpisodes.map {
            UpdateEpisode(
                id = it.id,
                url = it.url,
                name = it.name,
                episodeNumber = it.episodeNumber,
                dateUpload = it.dateUpload,
                sourceOrder = it.sourceOrder,
                languages = it.languages,
            )
        }

        // One transaction, so a cancel or an error can't delete the removed episodes without adding the new ones
        val addedEpisodes = episodeRepository.sync(
            toAdd = toAdd,
            toUpdate = episodeUpdates,
            toDelete = removedEpisodes.map { it.id },
        )
        awaitUpdateFetchInterval(anime, now, fetchWindow)

        // Set this anime as updated since episodes were changed
        // Note that last_update actually represents last time the episode list changed at all
        awaitUpdateLastUpdate(anime.id)

        return addedEpisodes
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

    suspend fun awaitUpdateLastUpdate(animeId: Long): Boolean {
        return animeRepository.updateAnime(UpdateAnime(id = animeId, lastUpdate = Instant.now().toEpochMilli()))
    }

}