package com.sf.animescraper.domain.episode

import com.sf.animescraper.network.api.model.SEpisode

data class Episode(
    val id : Long,
    val animeId : Long,
    val url : String,
    val name : String,
    val episodeNumber : Float,
    val seen : Boolean,
    val date : String?,
) {
    companion object {
        fun create() : Episode{
            return Episode(
                id = -1L,
                animeId = -1L,
                url = "",
                name = "",
                seen = false,
                episodeNumber = -1F,
                date = null
            )
        }
    }
}

fun Episode.copyFromSEpisode(other : SEpisode) : Episode{
    return this.copy(
        url = other.url,
        name = other.name,
        episodeNumber = other.episodeNumber,
        date = other.date
    )
}
