package com.sf.tadami.domain.episode

import com.sf.tadami.source.model.SEpisode

data class Episode(
    val id : Long,
    val animeId : Long,
    val url : String,
    val name : String,
    val timeSeen : Long,
    val totalTime : Long,
    val dateFetch : Long,
    val dateUpload : Long,
    val episodeNumber : Float,
    val seen : Boolean,
    val sourceOrder : Long,
    val languages : String?,
    val seasonName : String? = null,
    val seasonNumber : Float? = null
) {

    fun copyFrom(other: Episode): Episode {
        return copy(
            name = other.name,
            url = other.url,
            dateUpload = other.dateUpload,
            episodeNumber = other.episodeNumber,
            seasonName = other.seasonName,
            seasonNumber = other.seasonNumber
        )
    }
    companion object {
        fun create() : Episode{
            return Episode(
                id = -1L,
                animeId = -1L,
                url = "",
                name = "",
                episodeNumber = -1F,
                timeSeen = 0,
                totalTime = 0,
                dateFetch = 0,
                dateUpload = -1,
                seen = false,
                sourceOrder = 0L,
                languages = null,
                seasonName = null,
                seasonNumber = null
            )
        }
    }
}

fun Episode.toSEpisode() : SEpisode{
    return SEpisode.create().apply {
        url = this@toSEpisode.url
        name = this@toSEpisode.name
        episodeNumber = this@toSEpisode.episodeNumber
        dateUpload = this@toSEpisode.dateUpload
        languages = this@toSEpisode.languages
        seasonName = this@toSEpisode.seasonName
        seasonNumber = this@toSEpisode.seasonNumber
    }
}

fun Episode.copyFromSEpisode(other : SEpisode) : Episode{
    return this.copy(
        url = other.url,
        name = other.name,
        episodeNumber = other.episodeNumber,
        dateUpload = other.dateUpload,
        languages = other.languages,
        seasonName = other.seasonName,
        seasonNumber = other.seasonNumber
    )
}
