package com.sf.tadami.data.anime

import com.sf.tadami.domain.anime.LibraryAnime

val libraryMapper: (
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
    episodes : Long,
    unseen_episodes: Double
) -> LibraryAnime =
    { id: Long,
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
      episodes : Long,
      unseenEpisodes: Double
        ->
        LibraryAnime(
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
            episodes = episodes,
            unseenEpisodes = unseenEpisodes.toLong()
        )
    }