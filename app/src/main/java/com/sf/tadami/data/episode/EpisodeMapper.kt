package com.sf.tadami.data.episode

import com.sf.tadami.domain.episode.Episode

val episodeMapper: (
    _id: Long,
    anime_id: Long,
    url: String,
    name: String,
    episode_number: Double,
    time_seen : Long,
    total_time : Long,
    date_fetch: Long,
    date_upload: Long,
    seen: Boolean,
    source_order: Long
) -> Episode =
    { id: Long,
      animeId: Long,
      url: String,
      name: String,
      episodeNumber: Double,
      timeSeen : Long,
      totalTime : Long,
      dateFetch : Long,
      dateUpload : Long,
      seen: Boolean,
      sourceOrder : Long
        ->
        Episode(
            id = id,
            animeId = animeId,
            url = url,
            name = name,
            episodeNumber = episodeNumber.toFloat(),
            timeSeen = timeSeen,
            totalTime = totalTime,
            dateFetch = dateFetch,
            dateUpload = dateUpload,
            seen = seen,
            sourceOrder = sourceOrder
        )
    }