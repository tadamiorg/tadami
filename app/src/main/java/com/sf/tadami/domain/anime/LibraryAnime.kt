package com.sf.tadami.domain.anime

import com.sf.tadami.source.model.SAnimeStatus

data class LibraryAnime(
    val id: Long,
    val source: Long,
    val url: String,
    val title: String,
    val rawTitle: String?,
    val thumbnailUrl: String?,
    val release: String?,
    val studio: String?,
    val author: String?,
    val status: SAnimeStatus,
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
