package com.sf.tadami.data.anime

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.domain.category.Category
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
        rawTitle: String?,
        language: String?,
        coverLastModified: Long,
    ) : Anime = Anime(
        id = id,
        source = source,
        url = url,
        title = title,
        rawTitle = rawTitle,
        language = language,
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
        dateAdded = dateAdded,
        coverLastModified = coverLastModified,
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
        language: String?,
        coverLastModified: Long,

        episodes : Long,
        unseenEpisodes: Double,
        categories: String
    ) : LibraryAnime = LibraryAnime(
        id = id,
        source = source,
        url = url,
        title = title,
        rawTitle = rawTitle,
        language = language,
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
        dateAdded = dateAdded,
        coverLastModified = coverLastModified,
        categories = parseCategories(categories)
    )

    // The view's comma separated category ids
    internal fun parseCategories(categories: String): List<Long> {
        return categories.split(',')
            .mapNotNull { it.trim().toLongOrNull() }
            .ifEmpty { listOf(Category.UNCATEGORIZED_ID) }
    }
}
