package com.sf.tadami.network.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@OptIn(UnstableApi::class)
class PlayerNetworkHelper(context : Context) {
    @UnstableApi
    private val databaseProvider = StandaloneDatabaseProvider(context)
    private val cacheSize = 400L * 1024 * 1024 // 400 MiB
    private val cacheDir = File(context.cacheDir, "player_network_cache")
    @UnstableApi
    var cache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(cacheSize), databaseProvider)

    fun clearCache(){
        cache.release()
        SimpleCache.delete(cacheDir,databaseProvider)
        cache = SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(cacheSize), databaseProvider)
    }
}