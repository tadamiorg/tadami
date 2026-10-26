package com.sf.tadami.ui.utils

import com.sf.tadami.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// The values of the Media3 MimeTypes constants the Android app returns
private const val TEXT_UNKNOWN = "text/x-unknown"
private const val APPLICATION_SUBRIP = "application/x-subrip"
private const val TEXT_VTT = "text/vtt"
private const val APPLICATION_TTML = "application/ttml+xml"
private const val TEXT_SSA = "text/x-ssa"

object SubtitleFormats {
    val unknownFormat: String = TEXT_UNKNOWN
}

suspend fun detectSubtitleFormat(subtitleUrl: String): String {
    when (subtitleUrl.substringAfterLast('.', "").lowercase()) {
        "srt" -> return APPLICATION_SUBRIP
        "vtt", "webvtt" -> return TEXT_VTT
        "ttml", "xml" -> return APPLICATION_TTML
        "ssa", "ass" -> return TEXT_SSA
    }
    return withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(subtitleUrl)
                .header("Range", "bytes=0-1000")
                .build()
            Injekt.get<NetworkHelper>().client.newCall(request).execute().use { response ->
                val content = response.body.string()
                when {
                    content.trimStart().startsWith("WEBVTT") -> TEXT_VTT
                    content.contains("<?xml") && content.contains("<tt") -> APPLICATION_TTML
                    content.contains("[Script Info]") -> TEXT_SSA
                    content.contains("-->") -> APPLICATION_SUBRIP
                    else -> TEXT_UNKNOWN
                }
            }
        } catch (e: Exception) {
            TEXT_UNKNOWN
        }
    }
}
