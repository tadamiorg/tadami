package com.sf.tadami.domain.anime

import com.sf.tadami.domain.category.Category
import com.sf.tadami.source.model.SAnimeStatus

data class LibraryAnime(
    val id: Long,
    val source: Long,
    val url: String,
    val title: String,
    val rawTitle: String?,
    val language: String?,
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
    val dateAdded : Long,
    // When its cover was last refreshed, part of the cover's image cache key
    val coverLastModified: Long = 0L,
    // The ids of its categories, the Default category's one when it is in none
    val categories : List<Long> = listOf(Category.UNCATEGORIZED_ID)
)
