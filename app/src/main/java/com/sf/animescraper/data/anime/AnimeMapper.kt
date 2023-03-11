package com.sf.animescraper.data.anime

import com.sf.animescraper.domain.anime.Anime

val animeMapper: (
    _id: Long,
    source: String,
    url: String,
    title: String,
    thumbnail_url: String?,
    release: String?,
    status: String?,
    description: String?,
    genres: List<String>?,
    favorite: Boolean,
    initialized: Boolean,
) -> Anime =
    {
            id: Long,
            source: String,
            url: String,
            title: String,
            thumbnailUrl: String?,
            release: String?,
            status: String?,
            description: String?,
            genres: List<String>?,
            favorite: Boolean,
            initialized: Boolean,
        ->
        Anime(
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
        )
    }