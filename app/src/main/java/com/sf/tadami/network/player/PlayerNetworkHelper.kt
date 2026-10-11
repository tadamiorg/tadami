package com.sf.tadami.network.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.sf.tadami.network.NetworkHelper
import okhttp3.OkHttpClient
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
class PlayerNetworkHelper(context : Context) {
    @UnstableApi
    private val databaseProvider = StandaloneDatabaseProvider(context)
    private val cacheSize = 400L * 1024 * 1024 // 400 MiB
    private val cacheDir = File(context.cacheDir, "player_network_cache")
    @UnstableApi
    var cache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(cacheSize), databaseProvider)

    /**
     * Streams go through OkHttp rather than HttpURLConnection: it speaks HTTP/2, which some video
     * hosts require (Kwik's firewall answers 403 to HTTP/1.1), and shares the app's cookies.
     */
    val okHttpClient: OkHttpClient by lazy {
        Injekt.get<NetworkHelper>().nonCloudflareClient.newBuilder()
            // The player caches the media itself
            .cache(null)
            // One call can stream a whole progressive video
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    fun clearCache(){
        cache.release()
        SimpleCache.delete(cacheDir,databaseProvider)
        cache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(cacheSize), databaseProvider)
    }
}