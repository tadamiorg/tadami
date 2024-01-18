package com.sf.tadami.domain.anime

data class LibraryAnime(
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
    val lastUpdate: Long,
    val nextUpdate: Long,
    val fetchInterval: Int,
    val unseenEpisodes: Long,
    val episodeFlags : Long,
    val dateAdded : Long
)
