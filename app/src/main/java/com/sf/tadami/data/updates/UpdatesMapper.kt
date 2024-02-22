package com.sf.tadami.data.updates

import com.sf.tadami.domain.updates.UpdatesWithRelations

object UpdatesMapper {

    fun mapUpdatesWithRelations(
        animeId: Long,
        animeTitle: String,
        episodeId: Long,
        episodeName: String,
        seen: Boolean,
        timeSeen: Long,
        totalTime : Long,
        sourceId: Long,
        favorite: Boolean,
        thumbnailUrl: String?,
        dateUpload: Long,
        dateFetch: Long,

    ): UpdatesWithRelations = UpdatesWithRelations(
        animeId = animeId,
        episodeId = episodeId,
        animeTitle = animeTitle,
        episodeName = episodeName,
        seen = seen,
        timeSeen = timeSeen,
        totalTime = totalTime,
        sourceId = sourceId,
        dateFetch = dateFetch,
        thumbnailUrl = thumbnailUrl
    )
}