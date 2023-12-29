package com.sf.tadami.domain.anime

data class UpdateAnime(
    val id : Long,
    val source: String? = null,
    val url: String? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val release: String? = null,
    val status: String? = null,
    val description: String? = null,
    val genres: List<String>? = null,
    val favorite: Boolean? = null,
    val initialized: Boolean? = null,
    val lastUpdate: Long? = null,
    val nextUpdate: Long? = null,
    val fetchInterval: Int? = null,
    val episodeFlags: Long? = null
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
        thumbnailUrl = thumbnailUrl,
        release = release,
        status = status,
        description = description,
        genres = genres,
        favorite = favorite,
        initialized = initialized,
        lastUpdate = lastUpdate,
        nextUpdate = nextUpdate,
        fetchInterval = fetchInterval,
        episodeFlags = episodeFlags
    )
}