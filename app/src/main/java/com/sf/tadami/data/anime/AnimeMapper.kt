package com.sf.tadami.data.anime

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.LibraryAnime

object AnimeMapper {
    fun mapAnime(
        id: Long,
        source: String,
        url: String,
        title: String,
        thumbnailUrl: String?,
        release: String?,
        status: String?,
        description: String?,
        genres: List<String>?,
        lastUpdate : Long?,
        nextUpdate : Long?,
        calculateInterval : Long,
        favorite: Boolean,
        initialized: Boolean,
        episodeFlags : Long
    ) : Anime = Anime(
        id = id,
        source = source,
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        release = release,
        status = status,
        description = description,
        genres = genres,
        favorite = favorite,
        initialized = initialized,
        lastUpdate = lastUpdate ?: 0,
        nextUpdate = nextUpdate ?: 0,
        fetchInterval = calculateInterval.toInt(),
        episodeFlags = episodeFlags
    )

    fun mapLibraryAnime(
        id: Long,
        source: String,
        url: String,
        title: String,
        thumbnailUrl: String?,
        release: String?,
        status: String?,
        description: String?,
        genres: List<String>?,
        lastUpdate : Long?,
        nextUpdate : Long?,
        calculateInterval : Long,
        favorite: Boolean,
        initialized: Boolean,
        episodeFlags : Long,

        episodes : Long,
        unseenEpisodes: Double
    ) : LibraryAnime = LibraryAnime(
        id = id,
        source = source,
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        release = release,
        status = status,
        description = description,
        genres = genres,
        favorite = favorite,
        initialized = initialized,
        lastUpdate = lastUpdate ?: 0,
        nextUpdate = nextUpdate ?: 0,
        fetchInterval = calculateInterval.toInt(),
        episodes = episodes,
        unseenEpisodes = unseenEpisodes.toLong(),
        episodeFlags = episodeFlags
    )
}