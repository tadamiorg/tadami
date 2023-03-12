package com.sf.animescraper.domain.episode

data class UpdateEpisode(
    val id: Long,
    val animeId: Long? = null,
    val url: String? = null,
    val name: String? = null,
    val episodeNumber: Float? =  null,
    val seen: Boolean? = null,
    val date: String? =  null,
    val sourceOrder : Long? = null
)


fun Episode.toUpdateEpisode() : UpdateEpisode{
    return UpdateEpisode(
        id = id,
        animeId = animeId,
        url = url,
        name = name,
        episodeNumber = episodeNumber,
        seen = seen,
        date = date,
        sourceOrder = sourceOrder
    )
}