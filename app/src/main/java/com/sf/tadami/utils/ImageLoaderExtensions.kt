package com.sf.tadami.utils

import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi

/**
 * Drops the image of [url] from the memory and disk caches, so its next load downloads it again. The covers are
 * loaded from their URL, which is their key in both caches. The other images stay.
 */
@OptIn(ExperimentalCoilApi::class)
fun ImageLoader.invalidate(url: String) {
    memoryCache?.let { cache ->
        // One entry per transformation and size too
        cache.keys.filter { it.key == url }.forEach { cache.remove(it) }
    }
    diskCache?.remove(url)
}
