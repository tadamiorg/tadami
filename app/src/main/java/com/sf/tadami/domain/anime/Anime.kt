package com.sf.tadami.domain.anime

import com.sf.tadami.network.api.model.SAnime
import data.Anime as AnimeDb

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
    val lastUpdate: Long,
    val nextUpdate: Long,
    val fetchInterval: Int,
    val initialized: Boolean,
) {
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

    fun copyFrom(other: AnimeDb): Anime {
        var anime = this
        other.release?.let { anime = anime.copy(release = it) }
        other.description?.let { anime = anime.copy(description = it) }
        other.genres?.let { anime = anime.copy(genres = it) }
        other.thumbnail_url?.let { anime = anime.copy(thumbnailUrl = it) }
        anime = anime.copy(status = other.status)
        if (!initialized) {
            anime = anime.copy(initialized = other.initialized)
        }
        return anime
    }

    companion object {
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
            initialized = false,
            lastUpdate = 0L,
            nextUpdate = 0L,
            fetchInterval = 0,
        )
    }
}

fun SAnime.toDomainAnime(source: String): Anime {
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

fun LibraryAnime.toAnime(): Anime {
    return Anime(
        id = id,
        source = source,
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        release = release,
        status = status,
        description = description,
        genres = genres,
        favorite = favorite,
        initialized = initialized,
        fetchInterval = fetchInterval,
        lastUpdate = lastUpdate,
        nextUpdate = nextUpdate
    )
}

