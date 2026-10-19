package com.sf.tadami.domain.anime

/**
 * The cover of an anime to load, as Mihon's MangaCover: [lastModified] is part of its image cache key, so a refreshed
 * cover is loaded again even at the same [url], as soon as it's shown.
 */
data class AnimeCoverData(
    val url: String,
    val lastModified: Long,
) {
    // The URL alone while never refreshed, the key the covers had before, so their caches stay
    val cacheKey: String
        get() = if (lastModified == 0L) url else "$url;$lastModified"

    companion object {
        // Null without URL: nothing to load, the cover shows its error image
        fun of(url: String?, lastModified: Long): AnimeCoverData? {
            return url?.takeIf { it.isNotEmpty() }?.let { AnimeCoverData(it, lastModified) }
        }
    }
}

val Anime.coverData: AnimeCoverData?
    get() = AnimeCoverData.of(thumbnailUrl, coverLastModified)
