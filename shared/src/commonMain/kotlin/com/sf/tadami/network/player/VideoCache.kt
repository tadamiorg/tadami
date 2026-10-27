package com.sf.tadami.network.player

/** The cache of the player's streams, registered in Injekt. */
interface VideoCache {

    /** The bytes it takes. */
    val cacheSpace: Long

    fun clearCache()
}
