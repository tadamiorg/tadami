package com.sf.tadami.data.anime

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.source.model.SAnimeStatus

object AnimeMapper {
    fun mapAnime(
        id: Long,
        source: Long,
        url: String,
        title: String,
        thumbnailUrl: String?,
        release: String?,
        status: SAnimeStatus?,
        description: String?,
        genres: List<String>?,
        lastUpdate : Long?,
        nextUpdate : Long?,
        calculateInterval : Long,
        favorite: Boolean,
        initialized: Boolean,
        episodeFlags : Long,
        dateAdded : Long,
        studio: String?,
        author: String?,
        rawTitle: String?
    ) : Anime = Anime(
        id = id,
        source = source,
        url = url,
        title = title,
        rawTitle = rawTitle,
        thumbnailUrl = thumbnailUrl,
        release = release,
        studio = studio,
        author = author,
        status = status ?: SAnimeStatus.UNKNOWN,
        description = description,
        genres = genres,
        favorite = favorite,
        initialized = initialized,
        lastUpdate = lastUpdate ?: 0,
        nextUpdate = nextUpdate ?: 0,
        fetchInterval = calculateInterval.toInt(),
        episodeFlags = episodeFlags,
        dateAdded = dateAdded
    )

    fun mapLibraryAnime(
        id: Long,
        source: Long,
        url: String,
        title: String,
        thumbnailUrl: String?,
        release: String?,
        status: SAnimeStatus?,
        description: String?,
        genres: List<String>?,
        lastUpdate : Long?,
        nextUpdate : Long?,
        calculateInterval : Long,
        favorite: Boolean,
        initialized: Boolean,
        episodeFlags : Long,
        dateAdded: Long,
        studio: String?,
        author: String?,
        rawTitle: String?,

        episodes : Long,
        unseenEpisodes: Double
    ) : LibraryAnime = LibraryAnime(
        id = id,
        source = source,
        url = url,
        title = title,
        rawTitle = rawTitle,
        thumbnailUrl = thumbnailUrl,
        release = release,
        studio = studio,
        author = author,
        status = status ?: SAnimeStatus.UNKNOWN,
        description = description,
        genres = genres,
        favorite = favorite,
        initialized = initialized,
        lastUpdate = lastUpdate ?: 0,
        nextUpdate = nextUpdate ?: 0,
        fetchInterval = calculateInterval.toInt(),
        episodes = episodes,
        unseenEpisodes = unseenEpisodes.toLong(),
        episodeFlags = episodeFlags,
        dateAdded = dateAdded
    )
}
