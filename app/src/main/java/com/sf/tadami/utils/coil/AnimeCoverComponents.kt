package com.sf.tadami.utils.coil

import coil.ImageLoader
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import com.sf.tadami.domain.anime.AnimeCoverData

/** The memory cache key of a cover, its URL and when it was refreshed, as Mihon's MangaCoverKeyer */
class AnimeCoverKeyer : Keyer<AnimeCoverData> {
    override fun key(data: AnimeCoverData, options: Options): String = data.cacheKey
}

/**
 * Loads a cover as its URL, with the regular fetchers, under its [AnimeCoverData.cacheKey] in the disk cache: a
 * refreshed cover is downloaded again even at the same URL.
 */
class AnimeCoverFetcherFactory : Fetcher.Factory<AnimeCoverData> {
    override fun create(data: AnimeCoverData, options: Options, imageLoader: ImageLoader): Fetcher? {
        val keyed = options.copy(diskCacheKey = options.diskCacheKey ?: data.cacheKey)
        val mapped = imageLoader.components.map(data.url, keyed)
        return imageLoader.components.newFetcher(mapped, keyed, imageLoader)?.first
    }
}
