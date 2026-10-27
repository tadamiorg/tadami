package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** The paths of the local route, the downloaded files the cast proxy serves. */
object LocalMediaPaths {
    const val PATH_PREFIX = "/local-"
}

/** Whether [url] is one of the local route's: downloaded files the proxy serves during a cast only. */
fun isLocalMediaUrl(url: String): Boolean =
    url.toHttpUrlOrNull()?.encodedPath?.startsWith(LocalMediaPaths.PATH_PREFIX) == true
