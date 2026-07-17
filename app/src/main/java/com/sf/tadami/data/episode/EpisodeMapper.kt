package com.sf.tadami.data.episode

import com.sf.tadami.data.backup.models.BackupEpisode
import com.sf.tadami.domain.episode.Episode

object EpisodeMapper {
    fun mapEpisode(
        id: Long,
        animeId: Long,
        url: String,
        name: String,
        episodeNumber: Double,
        timeSeen : Long,
        totalTime : Long,
        dateFetch: Long,
        dateUpload: Long,
        seen: Boolean,
        sourceOrder: Long,
        languages : String?,
        seasonName : String?,
        seasonNumber : Double?
    ) : Episode = Episode(
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
        sourceOrder = sourceOrder,
        languages = languages,
        seasonName = seasonName,
        seasonNumber = seasonNumber?.toFloat()
    )


    fun mapBackupEpisode(
        id: Long,
        animeId: Long,
        url: String,
        name: String,
        episodeNumber: Double,
        timeSeen : Long,
        totalTime : Long,
        dateFetch: Long,
        dateUpload: Long,
        seen: Boolean,
        sourceOrder: Long,
        languages : String?,
        seasonName : String?,
        seasonNumber : Double?
    ) : BackupEpisode = BackupEpisode(
        url = url,
        name = name,
        episodeNumber = episodeNumber.toFloat(),
        timeSeen = timeSeen,
        totalTime = totalTime,
        dateFetch = dateFetch,
        dateUpload = dateUpload,
        seen = seen,
        sourceOrder = sourceOrder,
        languages = languages,
        seasonName = seasonName,
        seasonNumber = seasonNumber?.toFloat()
    )
}