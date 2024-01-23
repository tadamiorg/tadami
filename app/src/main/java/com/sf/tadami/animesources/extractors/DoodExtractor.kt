package com.sf.tadami.animesources.extractors

import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.network.GET
import okhttp3.Headers
import okhttp3.OkHttpClient

class DoodExtractor(private val client: OkHttpClient) {

    fun videoFromUrl(
        url: String,
        quality: String? = null,
        redirect: Boolean = true,
    ): StreamSource? {
        val newQuality = quality ?: ("Doodstream" + if (redirect) " mirror" else "")

        return runCatching {
            val response = client.newCall(GET(url)).execute()
            val newUrl = if (redirect) response.request.url.toString() else url

            val doodHost = Regex("https://(.*?)/").find(newUrl)!!.groupValues[1]
            val content = response.body.string()
            if (!content.contains("'/pass_md5/")) return null
            val md5 = content.substringAfter("'/pass_md5/").substringBefore("',")
            val token = md5.substringAfterLast("/")
            val randomString = getRandomString()
            val expiry = System.currentTimeMillis()
            val videoUrlStart = client.newCall(
                GET(
                    "https://$doodHost/pass_md5/$md5",
                    Headers.headersOf("referer", newUrl),
                ),
            ).execute().body.string()
            val videoUrl = "$videoUrlStart$randomString?token=$token&expiry=$expiry"
            StreamSource(videoUrl, newQuality, headers = doodHeaders(doodHost))
        }.getOrNull()
    }

    fun videosFromUrl(
        url: String,
        quality: String? = null,
        redirect: Boolean = true,
    ): List<StreamSource> {
        val video = videoFromUrl(url, quality, redirect)
        return video?.let(::listOf) ?: emptyList<StreamSource>()
    }

    private fun getRandomString(length: Int = 10): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun doodHeaders(host: String) = Headers.Builder().apply {
        add("User-Agent", "Tadami")
        add("Referer", "https://$host/")
    }.build()
}