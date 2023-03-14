package com.sf.animescraper.domain.episode

import com.sf.animescraper.network.api.model.SEpisode

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
) {
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
            )
        }
    }
}

fun Episode.copyFromSEpisode(other : SEpisode) : Episode{
    return this.copy(
        url = other.url,
        name = other.name,
        episodeNumber = other.episodeNumber,
        dateUpload = other.dateUpload
    )
}
