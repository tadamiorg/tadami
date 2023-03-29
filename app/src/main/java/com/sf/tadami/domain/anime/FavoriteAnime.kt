package com.sf.tadami.domain.anime

data class FavoriteAnime(
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
    val episodes : Long,
    val unseenEpisodes: Long
)
