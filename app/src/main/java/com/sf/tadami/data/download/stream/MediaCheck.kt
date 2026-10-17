package com.sf.tadami.data.download.stream

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

/** Tells video from error pages and placeholders, and the container of what is downloaded. */
@OptIn(UnstableApi::class)
object MediaCheck {

    /** A real episode is bigger. A host served a 36 kB placeholder clip instead of the episode. */
    const val MIN_VIDEO_BYTES = 1L * 1024 * 1024

    /** Shorter is a placeholder or a preview, not an episode */
    const val MIN_DURATION_MS = 30_000L

    /** Bytes to read to sniff a container */
    const val SNIFF_BYTES = 64

    private val VIDEO_EXTENSIONS = setOf("mp4", "m4v", "mkv", "webm", "ts", "flv", "avi", "mov")

    fun isTextContentType(contentType: String?): Boolean {
        val type = contentType?.substringBefore(';')?.trim()?.lowercase() ?: return false
        return type.startsWith("text/") || type == "application/json" || type == "application/xml" ||
            type == "application/xhtml+xml" || type == "application/javascript"
    }

    fun isPlaylistContentType(contentType: String?): Boolean {
        return contentType?.lowercase()?.contains("mpegurl") == true
    }

    fun isPlaylistUrl(url: String): Boolean {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return path.endsWith(".m3u8") || path.endsWith(".m3u")
    }

    fun isPlaylist(head: ByteArray): Boolean {
        return head.decodeToString().trimStart('﻿', ' ', '\t', '\r', '\n').startsWith("#EXTM3U")
    }

    /** An HTML page, JSON or other text where a video was expected. */
    fun looksLikeText(head: ByteArray): Boolean {
        if (head.isEmpty()) return true
        val text = head.decodeToString().trimStart('﻿', ' ', '\t', '\r', '\n')
        if (text.isEmpty()) return true
        if (text[0] !in "<{[") return false
        // Markup or JSON is printable text, a container's header isn't
        return head.all { it >= 0x20 || it == 0x09.toByte() || it == 0x0A.toByte() || it == 0x0D.toByte() || it < 0 }
    }

    /** The extension of a progressive video, from its type, its URL, then its first bytes. */
    fun videoExtension(contentType: String?, url: String, head: ByteArray): String {
        when (contentType?.substringBefore(';')?.trim()?.lowercase()) {
            "video/mp4" -> return "mp4"
            "video/x-matroska" -> return "mkv"
            "video/webm" -> return "webm"
            "video/mp2t" -> return "ts"
            "video/x-flv" -> return "flv"
            "video/x-msvideo" -> return "avi"
            "video/quicktime" -> return "mov"
        }
        val urlExtension = url.substringBefore('?').substringBefore('#').substringAfterLast('/')
            .substringAfterLast('.', "").lowercase()
        if (urlExtension in VIDEO_EXTENSIONS) return urlExtension
        return sniffContainer(head) ?: "mp4"
    }

    /** The extension of joined HLS segments: fMP4 with an init segment, else TS or packed audio. */
    fun hlsExtension(fragmentedMp4: Boolean, audio: Boolean, firstSegment: ByteArray): String {
        if (fragmentedMp4) return if (audio) "m4a" else "mp4"
        return when (sniffContainer(firstSegment)) {
            "aac" -> "aac"
            "mp4" -> if (audio) "m4a" else "mp4"
            else -> "ts"
        }
    }

    internal fun sniffContainer(head: ByteArray): String? {
        fun at(index: Int) = head.getOrNull(index)?.toInt()?.and(0xFF)
        return when {
            // TS packets start with a sync byte, every 188 bytes
            at(0) == 0x47 -> "ts"
            head.size >= 8 && head.decodeToString(4, 8) in setOf("ftyp", "styp", "moof", "sidx") -> "mp4"
            at(0) == 0x1A && at(1) == 0x45 && at(2) == 0xDF && at(3) == 0xA3 -> "mkv"
            head.size >= 3 && head.decodeToString(0, 3) == "FLV" -> "flv"
            // Packed audio: an ID3 tag with the timestamp, or an ADTS frame
            head.size >= 3 && head.decodeToString(0, 3) == "ID3" -> "aac"
            at(0) == 0xFF && (at(1) ?: 0) and 0xF6 == 0xF0 -> "aac"
            else -> null
        }
    }

    fun subtitleExtension(mimeType: String?, url: String, head: ByteArray): String {
        when (mimeType) {
            MimeTypes.TEXT_VTT -> return "vtt"
            MimeTypes.APPLICATION_SUBRIP -> return "srt"
            MimeTypes.TEXT_SSA -> return "ass"
            MimeTypes.APPLICATION_TTML -> return "ttml"
        }
        when (val extension = url.substringBefore('?').substringAfterLast('.', "").lowercase()) {
            "vtt", "srt", "ass", "ssa", "ttml", "dfxp" -> return extension
            "webvtt" -> return "vtt"
        }
        val text = head.decodeToString()
        return when {
            text.trimStart('﻿', ' ', '\r', '\n').startsWith("WEBVTT") -> "vtt"
            "[Script Info]" in text -> "ass"
            "<tt" in text -> "ttml"
            "-->" in text -> "srt"
            else -> "vtt"
        }
    }

    fun subtitleMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "vtt" -> MimeTypes.TEXT_VTT
            "srt" -> MimeTypes.APPLICATION_SUBRIP
            "ass", "ssa" -> MimeTypes.TEXT_SSA
            "ttml", "dfxp" -> MimeTypes.APPLICATION_TTML
            else -> MimeTypes.TEXT_UNKNOWN
        }
    }

    fun isTooSmall(bytes: Long): Boolean = bytes < MIN_VIDEO_BYTES

    /** Null when the duration is unknown, TS files often give none. */
    fun isTooShort(durationMs: Long?): Boolean = durationMs != null && durationMs in 1 until MIN_DURATION_MS
}
