package com.sf.animescraper.animesources.extractors

import com.sf.animescraper.network.requests.okhttp.GET
import com.sf.animescraper.network.requests.utils.asJsoup
import com.sf.animescraper.network.scraping.dto.crypto.StreamSource
import okhttp3.OkHttpClient

class OkruExtractor(private val client: OkHttpClient) {

    private fun fixQuality(quality: String): String {
        val qualities = listOf(
            Pair("full", "1080p"),
            Pair("hd", "720p"),
            Pair("sd", "480p"),
            Pair("low", "360p"),
            Pair("lowest", "240p"),
            Pair("mobile", "144p")
        )
        return qualities.find { it.first == quality }?.second ?: quality
    }

    fun videosFromUrl(url: String, prefix: String = "", fixQualities: Boolean = true): List<StreamSource> {
        val document = client.newCall(GET(url)).execute().asJsoup()
        val videosString = document.selectFirst("div[data-options]")
            ?.attr("data-options")
            ?.substringAfter("\\\"videos\\\":[{\\\"name\\\":\\\"")
            ?.substringBefore("]")
            ?: return emptyList()
        return videosString.split("{\\\"name\\\":\\\"").reversed().mapNotNull {
            val videoUrl = it.substringAfter("url\\\":\\\"")
                .substringBefore("\\\"")
                .replace("\\\\u0026", "&")
            val quality = it.substringBefore("\\\"").let {
                if (fixQualities) fixQuality(it)
                else it
            }
            val videoQuality = ("Okru:$quality").let {
                if (prefix.isNotBlank()) "$prefix $it"
                else it
            }
            if (videoUrl.startsWith("https://"))
                StreamSource(videoUrl, videoQuality)
            else null
        }
    }
}