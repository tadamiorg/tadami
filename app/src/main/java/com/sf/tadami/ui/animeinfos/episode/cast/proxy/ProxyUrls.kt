package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLEncoder

/**
 * Proxied form of a cast-metadata image URL.
 *
 * The Cast SDK's image fetcher (the sender's mini/expanded controller and the media notification)
 * and the receiver's `<img>` both request the cover with NO Referer — browsers can't set one at
 * all — so hosts that hotlink-protect their images refuse it and the cover silently goes missing.
 * Routing it through the local proxy with a self-referer mirrors exactly what the app's own image
 * pipeline does for covers (`network/interceptors/ImageLoaderInterceptor`).
 *
 * Returns null when there is nothing to proxy (no running proxy / unusable url) so callers keep
 * the raw url: that also means the proxied form only exists for the cast session's lifetime,
 * which is precisely when the cast widget showing it exists.
 */
fun proxiedImageUrl(proxyBaseUrl: String?, imageUrl: String): String? {
    val base = proxyBaseUrl?.trimEnd('/')?.takeIf { it.isNotBlank() } ?: return null
    val httpUrl = imageUrl.toHttpUrlOrNull() ?: return null
    val headers = Json.encodeToString(mapOf("Referer" to "${httpUrl.scheme}://${httpUrl.host}"))
    return "$base/proxy?url=${enc(imageUrl)}&headers=${enc(headers)}"
}

private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
