package com.sf.tadami.domain.anime

import com.sf.tadami.source.model.SAnimeStatus

data class UpdateAnime(
    val id : Long,
    val source: Long? = null,
    val url: String? = null,
    val title: String? = null,
    val rawTitle: String? = null,
    val thumbnailUrl: String? = null,
    val release: String? = null,
    val studio: String? = null,
    val author: String? = null,
    val status: SAnimeStatus? = null,
    val description: String? = null,
    val genres: List<String>? = null,
    val favorite: Boolean? = null,
    val initialized: Boolean? = null,
    val lastUpdate: Long? = null,
    val nextUpdate: Long? = null,
    val fetchInterval: Int? = null,
    val episodeFlags: Long? = null,
    val dateAdded: Long? = null
){
    companion object{
        fun create(id : Long) : UpdateAnime{
            return UpdateAnime(id)
        }
    }
}

fun Anime.toUpdateAnime() : UpdateAnime {
    return UpdateAnime(
        id = id,
        source = source,
        url = url,
        title = title,
        rawTitle = rawTitle,
        thumbnailUrl = thumbnailUrl,
        release = release,
        studio = studio,
        author = author,
        status = status,
        description = description,
        genres = genres,
        favorite = favorite,
        initialized = initialized,
        lastUpdate = lastUpdate,
        nextUpdate = nextUpdate,
        fetchInterval = fetchInterval,
        episodeFlags = episodeFlags,
        dateAdded = dateAdded
    )
}