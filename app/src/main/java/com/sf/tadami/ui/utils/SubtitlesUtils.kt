package com.sf.tadami.ui.utils

import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.sf.tadami.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@UnstableApi
object SubtitleFormats {
    val unknownFormat: String = MimeTypes.TEXT_UNKNOWN
}

@OptIn(UnstableApi::class)
suspend fun detectSubtitleFormat(subtitleUrl: String): String {
    val extension = subtitleUrl.substringAfterLast('.', "").lowercase()

    // Try to detect from extension first
    when (extension) {
        "srt" -> return MimeTypes.APPLICATION_SUBRIP
        "vtt", "webvtt" -> return MimeTypes.TEXT_VTT
        "ttml", "xml" -> return MimeTypes.APPLICATION_TTML
        "ssa", "ass" -> return MimeTypes.TEXT_SSA
    }

    // If extension is ambiguous, try to parse content
    return withContext(Dispatchers.IO) {
        try {
            val client = Injekt.get<NetworkHelper>().client
            val request = Request.Builder()
                .url(subtitleUrl)
                .header("Range", "bytes=0-1000") // Read first 1KB
                .build()

            client.newCall(request).execute().use { response ->
                val content = response.body.string() ?: ""

                when {
                    content.trimStart().startsWith("WEBVTT") -> MimeTypes.TEXT_VTT
                    content.contains("<?xml") && content.contains("<tt") -> MimeTypes.APPLICATION_TTML
                    content.contains("[Script Info]") -> MimeTypes.TEXT_SSA
                    content.contains("-->") -> MimeTypes.APPLICATION_SUBRIP
                    else -> MimeTypes.TEXT_UNKNOWN
                }
            }
        } catch (e: Exception) {
            MimeTypes.TEXT_UNKNOWN
        }
    }
}