package com.sf.tadami.data.download.stream

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException

/**
 * An HLS playlist, what the downloads read of it: the tags of Media3's HlsPlaylistParser they use,
 * parsed without it so that the desktop downloads the same way.
 */
sealed interface M3u8Playlist {
    /** Where it was read from: its relative URIs resolve against it. */
    val baseUri: String
}

/** A multivariant playlist: the variants of the stream and their audio renditions. */
data class M3u8Multivariant(
    override val baseUri: String,
    val variants: List<Variant>,
    val audios: List<Rendition>,
) : M3u8Playlist {

    data class Variant(
        val url: String,
        /** In pixels, 0 when unknown. */
        val height: Int,
        /** In bits per second, the average one when given, 0 when unknown. */
        val bitrate: Int,
        val audioGroupId: String?,
    )

    data class Rendition(
        val url: String?,
        val groupId: String,
        val name: String,
        val language: String?,
        val isDefault: Boolean,
    )
}

/** A media playlist: the segments of one rendition. */
data class M3u8Media(
    override val baseUri: String,
    val mediaSequence: Long,
    val segments: List<Segment>,
    val hasEndTag: Boolean,
    /** Whether a segment is encrypted with anything but plain AES-128: DRM. */
    val hasDrm: Boolean,
) : M3u8Playlist {

    /** The duration of the segments, in microseconds. */
    val durationUs: Long get() = segments.sumOf { it.durationUs }

    data class Segment(
        val url: String,
        val durationUs: Long,
        /** The bytes of [url] it is, null when it's the whole resource. */
        val byteRange: LongRange?,
        /** The key of its AES-128 encryption, null when it isn't encrypted. */
        val keyUri: String?,
        /** The IV of the key tag, in hex, null when it has none (the media sequence number then). */
        val encryptionIv: String?,
        val initialization: Initialization?,
        val hasGapTag: Boolean,
        /** How many discontinuities come before it. */
        val discontinuitySequence: Int,
    )

    data class Initialization(val url: String, val byteRange: LongRange?)
}

object M3u8 {

    /** Parses [text], read from [url]. @throws IOException when it isn't a playlist. */
    fun parse(url: String, text: String): M3u8Playlist {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.firstOrNull() != "#EXTM3U") throw IOException("Not an HLS playlist")
        return if (lines.any { it.startsWith("#EXT-X-STREAM-INF") }) parseMultivariant(url, lines) else parseMedia(url, lines)
    }

    private fun parseMultivariant(baseUri: String, lines: List<String>): M3u8Multivariant {
        val variants = mutableListOf<M3u8Multivariant.Variant>()
        val audios = mutableListOf<M3u8Multivariant.Rendition>()
        var pending: Map<String, String>? = null
        for (line in lines) {
            when {
                line.startsWith("#EXT-X-STREAM-INF:") -> pending = attributes(line.substringAfter(':'))
                line.startsWith("#EXT-X-MEDIA:") -> {
                    val attrs = attributes(line.substringAfter(':'))
                    if (attrs["TYPE"] == "AUDIO") {
                        audios += M3u8Multivariant.Rendition(
                            url = attrs["URI"]?.let { resolve(baseUri, it) },
                            groupId = attrs["GROUP-ID"].orEmpty(),
                            name = attrs["NAME"].orEmpty(),
                            language = attrs["LANGUAGE"],
                            isDefault = attrs["DEFAULT"] == "YES",
                        )
                    }
                }
                !line.startsWith("#") -> pending?.let { attrs ->
                    variants += M3u8Multivariant.Variant(
                        url = resolve(baseUri, line),
                        height = attrs["RESOLUTION"]?.substringAfter('x')?.toIntOrNull() ?: 0,
                        bitrate = (attrs["AVERAGE-BANDWIDTH"] ?: attrs["BANDWIDTH"])?.toIntOrNull() ?: 0,
                        audioGroupId = attrs["AUDIO"],
                    )
                    pending = null
                }
            }
        }
        return M3u8Multivariant(baseUri, variants, audios)
    }

    private fun parseMedia(baseUri: String, lines: List<String>): M3u8Media {
        var mediaSequence = 0L
        var hasEndTag = false
        var hasDrm = false
        val segments = mutableListOf<M3u8Media.Segment>()

        var durationUs = 0L
        var byteRange: Pair<Long, Long?>? = null
        var gap = false
        var discontinuities = 0
        var keyUri: String? = null
        var keyIv: String? = null
        var initialization: M3u8Media.Initialization? = null
        // Where the next range without an offset starts, of the resource of the last range
        var nextOffset = 0L
        var lastRangeUrl: String? = null

        fun rangeOf(url: String, length: Long, offset: Long?): LongRange {
            val start = offset ?: if (url == lastRangeUrl) nextOffset else 0L
            lastRangeUrl = url
            nextOffset = start + length
            return start until start + length
        }

        for (line in lines) {
            when {
                line.startsWith("#EXT-X-MEDIA-SEQUENCE:") -> mediaSequence = line.substringAfter(':').toLongOrNull() ?: 0L
                line.startsWith("#EXTINF:") -> {
                    val seconds = line.substringAfter(':').substringBefore(',').toDoubleOrNull() ?: 0.0
                    durationUs = (seconds * 1_000_000).toLong()
                }
                line.startsWith("#EXT-X-BYTERANGE:") -> {
                    val value = line.substringAfter(':')
                    byteRange = value.substringBefore('@').toLong() to value.substringAfter('@', "").toLongOrNull()
                }
                line == "#EXT-X-GAP" -> gap = true
                line == "#EXT-X-DISCONTINUITY" -> discontinuities++
                line == "#EXT-X-ENDLIST" -> hasEndTag = true
                line.startsWith("#EXT-X-KEY:") -> {
                    val attrs = attributes(line.substringAfter(':'))
                    val method = attrs["METHOD"]
                    val format = attrs["KEYFORMAT"]
                    when {
                        method == null || method == "NONE" -> {
                            keyUri = null
                            keyIv = null
                        }
                        method == "AES-128" && (format == null || format == "identity") -> {
                            keyUri = attrs["URI"]?.let { resolve(baseUri, it) }
                            keyIv = attrs["IV"]
                        }
                        else -> hasDrm = true
                    }
                }
                line.startsWith("#EXT-X-MAP:") -> {
                    val attrs = attributes(line.substringAfter(':'))
                    val url = attrs["URI"]?.let { resolve(baseUri, it) }
                    initialization = url?.let {
                        val range = attrs["BYTERANGE"]?.let { value ->
                            val length = value.substringBefore('@').toLong()
                            val offset = value.substringAfter('@', "").toLongOrNull() ?: 0L
                            offset until offset + length
                        }
                        M3u8Media.Initialization(it, range)
                    }
                }
                !line.startsWith("#") -> {
                    val url = resolve(baseUri, line)
                    segments += M3u8Media.Segment(
                        url = url,
                        durationUs = durationUs,
                        byteRange = byteRange?.let { (length, offset) -> rangeOf(url, length, offset) },
                        keyUri = keyUri,
                        encryptionIv = keyIv,
                        initialization = initialization,
                        hasGapTag = gap,
                        discontinuitySequence = discontinuities,
                    )
                    durationUs = 0L
                    byteRange = null
                    gap = false
                }
            }
        }
        return M3u8Media(baseUri, mediaSequence, segments, hasEndTag, hasDrm)
    }

    /** The attributes of a tag, `KEY=value,KEY="quoted, value"`. */
    fun attributes(list: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var i = 0
        while (i < list.length) {
            val eq = list.indexOf('=', i)
            if (eq < 0) break
            val key = list.substring(i, eq).trim()
            var j = eq + 1
            val value: String
            if (j < list.length && list[j] == '"') {
                val end = list.indexOf('"', j + 1).let { if (it < 0) list.length else it }
                value = list.substring(j + 1, end)
                j = end + 1
            } else {
                val end = list.indexOf(',', j).let { if (it < 0) list.length else it }
                value = list.substring(j, end).trim()
                j = end
            }
            result[key] = value
            i = list.indexOf(',', j).let { if (it < 0) list.length else it + 1 }
        }
        return result
    }

    /** [reference] against [base], as a browser resolves a link. */
    fun resolve(base: String, reference: String): String =
        base.toHttpUrlOrNull()?.resolve(reference)?.toString()
            ?: runCatching { java.net.URI(base).resolve(reference.replace(" ", "%20")).toString() }.getOrDefault(reference)
}
