package com.sf.tadami.ui.animeinfos.episode.cast.proxy

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers

/**
 * Header policy of the stateless proxy: forward the incoming request, overlay the headers that
 * came WITH the request (the receiver builds them from the stream source), and inject nothing
 * else beyond an optional User-Agent fallback.
 */

// The old ProxyServer's exclusions (host/origin/accept-language) plus entries that are not real
// request headers: NanoHTTPD injects http-client-ip/remote-addr into the same map, and
// connection/content-length are per-hop framing.
private val REQUEST_HEADER_EXCLUSIONS = setOf(
    "host", "origin", "accept-language",
    "connection", "content-length",
    "http-client-ip", "remote-addr",
)

/**
 * Tolerant parse of the `headers` query parameter: a JSON object of string values
 * (`{"Referer":"https://...","User-Agent":"..."}`). Returns null on null/blank/malformed input —
 * the proxy then just forwards the incoming request, like the old ProxyServer's fallback.
 */
fun parseHeadersParam(raw: String?): Headers? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        val builder = Headers.Builder()
        for ((name, value) in Json.parseToJsonElement(raw).jsonObject) {
            runCatching { builder.add(name, value.jsonPrimitive.content) }
        }
        builder.build()
    }.getOrNull()
}

/**
 * Upstream request headers: [incoming] minus [REQUEST_HEADER_EXCLUSIONS] (Range, Accept, If-*,
 * Accept-Encoding, … flow through untouched), then [requestHeaders] (the source headers carried by
 * the request) overlaid with them winning name conflicts, then [fallbackUserAgent] (the
 * AdvancedPreferences UA) only when no User-Agent survived.
 *
 * [incoming] keys arrive lowercased from NanoHTTPD; values OkHttp rejects are skipped.
 */
fun buildUpstreamHeaders(
    incoming: Map<String, String>,
    requestHeaders: Headers?,
    fallbackUserAgent: String,
): Headers {
    val builder = Headers.Builder()
    for ((name, value) in incoming) {
        if (name.lowercase() in REQUEST_HEADER_EXCLUSIONS) continue
        runCatching { builder.add(name, value) }
    }
    if (requestHeaders != null) {
        for (name in requestHeaders.names()) {
            builder.removeAll(name)
            for (value in requestHeaders.values(name)) runCatching { builder.add(name, value) }
        }
    }
    if (builder["User-Agent"] == null && fallbackUserAgent.isNotBlank()) {
        builder.add("User-Agent", fallbackUserAgent)
    }
    return builder.build()
}

// Only what the proxy frames itself: content-type goes through the NanoHTTPD response constructor,
// content-length/transfer-encoding/connection are NanoHTTPD's job, and cors() owns access-control-*.
// Everything else — Content-Range, Accept-Ranges, ETag, Content-Encoding, … — forwards verbatim
// (Accept-Encoding passes through upstream, so encoded bodies and their headers stay consistent).
private val RESPONSE_HEADER_EXCLUSIONS = setOf(
    "content-type", "content-length", "transfer-encoding", "connection", "keep-alive",
    "access-control-allow-origin", "access-control-allow-methods",
    "access-control-allow-headers", "access-control-expose-headers",
)

/** Upstream response headers to forward verbatim. */
fun forwardedResponseHeaders(upstream: Headers): List<Pair<String, String>> =
    upstream.filterNot { (name, _) -> name.lowercase() in RESPONSE_HEADER_EXCLUSIONS }
