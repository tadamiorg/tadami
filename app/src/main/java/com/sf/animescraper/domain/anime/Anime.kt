package com.sf.animescraper.domain.anime

import com.sf.animescraper.network.api.model.SAnime

data class Anime(
    val id: Long,
    val source: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String?,
    val release: String?,
    val status: String?,
    val description: String?,
    val genres: List<String>?,
    val favorite: Boolean,
    val initialized: Boolean,
){
    fun copyFrom(other: SAnime): Anime {
        return this.copy(
            release = other.release,
            description = other.description,
            genres = other.genres,
            thumbnailUrl = other.thumbnailUrl,
            status = other.status,
            initialized = other.initialized && initialized,
        )
    }


    companion object{
        fun create() = Anime(
            id = -1L,
            source = "",
            url = "",
            title = "",
            thumbnailUrl = null,
            release = null,
            status = null,
            description = null,
            genres = null,
            favorite = false,
            initialized = false
        )
    }
}

fun SAnime.toDomainAnime(source: String) : Anime{
    return Anime.create().copy(
        source = source,
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        release = release,
        status = status,
        description = description,
        genres = genres,
        initialized = initialized
    )
}

