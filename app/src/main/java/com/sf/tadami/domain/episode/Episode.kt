package com.sf.tadami.domain.episode

import com.sf.tadami.network.api.model.SEpisode

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
    val languages : String?
) {

    fun copyFrom(other: Episode): Episode {
        return copy(
            name = other.name,
            url = other.url,
            dateUpload = other.dateUpload,
            episodeNumber = other.episodeNumber
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
                languages = null
            )
        }
    }
}

fun Episode.copyFromSEpisode(other : SEpisode) : Episode{
    return this.copy(
        url = other.url,
        name = other.name,
        episodeNumber = other.episodeNumber,
        dateUpload = other.dateUpload,
        languages = other.languages
    )
}
