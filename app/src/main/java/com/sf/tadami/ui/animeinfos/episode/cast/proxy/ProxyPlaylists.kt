package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import okhttp3.Headers
import okhttp3.HttpUrl

/**
 * The one payload the proxy rewrites: HLS media playlists whose segment IVs players would get
 * wrong (see HlsSegmentIvs), which the receiver's player can't work around. Everything else, and
 * every playlist that doesn't need it, is forwarded as is. Playlists are recognised by their
 * bytes, never by URL or Content-Type.
 */

/** Largest playlist read whole to be fixed, anything bigger streams through untouched. */
internal const val MAX_FIXED_PLAYLIST_BYTES = 8L * 1024 * 1024

/** Bytes peeked at the start of a body to recognise a playlist. */
internal const val PLAYLIST_MAGIC_BYTES = 10L

private val PLAYLIST_MAGIC = "#EXTM3U".toByteArray(Charsets.US_ASCII)
private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

/**
 * Hint only, for the request: a URL that looks like a playlist is fetched without the receiver's
 * Accept-Encoding ([withoutAcceptEncoding]) so that its body arrives decoded, ready to be fixed.
 * Whether a response IS a playlist is decided from its bytes ([startsLikePlaylist]).
 */
fun isPlaylistUrl(url: HttpUrl): Boolean {
    val path = url.encodedPath.lowercase()
    return path.endsWith(".m3u8") || path.endsWith(".m3u")
}

/** Without Accept-Encoding, OkHttp asks for gzip itself and hands the body over decoded. */
fun withoutAcceptEncoding(headers: Headers): Headers =
    headers.newBuilder().removeAll("Accept-Encoding").build()

/**
 * Whether the upstream response could be a playlist to read and fix, before looking at its
 * bytes: a whole (not HEAD, not a range, 200) and decoded body, small enough to buffer.
 */
fun isWholeDecodedBody(
    method: String,
    rangeRequested: Boolean,
    code: Int,
    contentEncoding: String?,
    contentLength: Long,
): Boolean {
    if (method != "GET" || rangeRequested || code != 200) return false
    if (contentEncoding != null && !contentEncoding.equals("identity", ignoreCase = true)) return false
    return contentLength <= MAX_FIXED_PLAYLIST_BYTES
}

/** Whether a body starting with [prefix] is an HLS playlist (`#EXTM3U`, after an optional BOM). */
fun startsLikePlaylist(prefix: ByteArray): Boolean {
    val start = if (prefix.startsWith(UTF8_BOM)) UTF8_BOM.size else 0
    return prefix.size - start >= PLAYLIST_MAGIC.size &&
        PLAYLIST_MAGIC.indices.all { prefix[start + it] == PLAYLIST_MAGIC[it] }
}

private fun ByteArray.startsWith(bytes: ByteArray): Boolean =
    size >= bytes.size && bytes.indices.all { this[it] == bytes[it] }

// Validators of the upstream body no longer match a rewritten one
private val REWRITTEN_RESPONSE_HEADER_EXCLUSIONS = setOf("etag", "content-md5", "digest", "content-encoding")

/** Response headers to forward for a playlist served [rewritten] or as read from upstream. */
fun playlistResponseHeaders(forwarded: List<Pair<String, String>>, rewritten: Boolean): List<Pair<String, String>> =
    if (!rewritten) forwarded else forwarded.filterNot { (name, _) -> name.lowercase() in REWRITTEN_RESPONSE_HEADER_EXCLUSIONS }
