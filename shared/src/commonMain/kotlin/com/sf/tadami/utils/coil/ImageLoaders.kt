package com.sf.tadami.utils.coil

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.interceptors.ImageLoaderInterceptor
import kotlinx.coroutines.Dispatchers
import okio.Path

/**
 * The app's image loader: the covers of the database keyed by when they were last refreshed, through
 * the clients of the sources, with memory and disk caches.
 */
fun ImageLoader.Builder.tadamiDefaults(
    context: PlatformContext,
    network: NetworkHelper,
    diskCacheDir: Path,
    crossfadeMillis: Int,
): ImageLoader.Builder = this
    .components {
        // Shares the cookies (Cloudflare clearance) and user agent of the sources, without ever
        // opening a browser for an image
        add(
            OkHttpNetworkFetcherFactory(
                callFactory = {
                    network.nonCloudflareClient.newBuilder()
                        // Coil has its own disk cache
                        .cache(null)
                        // This header will be added to every image request.
                        .addInterceptor(ImageLoaderInterceptor())
                        .build()
                },
            ),
        )
        // The covers of the database, keyed by when they were last refreshed
        add(AnimeCoverKeyer())
        add(AnimeCoverFetcherFactory())
    }
    .crossfade(crossfadeMillis)
    .memoryCache {
        MemoryCache.Builder()
            .maxSizePercent(context, 0.25)
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(diskCacheDir)
            .maxSizePercent(0.02)
            .build()
    }
    .networkCachePolicy(CachePolicy.ENABLED)
    .fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(8))
    .decoderCoroutineContext(Dispatchers.IO.limitedParallelism(2))
