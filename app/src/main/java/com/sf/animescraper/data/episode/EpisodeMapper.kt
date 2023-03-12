package com.sf.animescraper.data.episode

import com.sf.animescraper.domain.episode.Episode

val episodeMapper: (
    _id: Long,
    anime_id: Long,
    url: String,
    name: String,
    episode_number: Double,
    seen: Boolean,
    date: String?,
    source_order: Long
) -> Episode =
    { id: Long,
      animeId: Long,
      url: String,
      name: String,
      episodeNumber: Double,
      seen: Boolean,
      date: String?,
      sourceOrder : Long
        ->
        Episode(
            id = id,
            animeId = animeId,
            url = url,
            name = name,
            episodeNumber = episodeNumber.toFloat(),
            seen = seen,
            date = date,
            sourceOrder = sourceOrder
        )
    }