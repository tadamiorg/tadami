package com.sf.tadami.animesources.extractors

import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class SibnetExtractor(private val client: OkHttpClient) {

    fun videosFromUrl(url: String, prefix: String = ""): List<StreamSource> {
        val videoList = mutableListOf<StreamSource>()

        val document = client.newCall(
            GET(url),
        ).execute().asJsoup()
        val script = document.selectFirst("script:containsData(player.src)")?.data() ?: return emptyList()
        val slug = script.substringAfter("player.src").substringAfter("src:")
            .substringAfter("\"").substringBefore("\"")

        val videoUrl = if (slug.contains("http")) {
            slug
        } else {
            "https://${url.toHttpUrl().host}$slug"
        }

        val videoHeaders = Headers.headersOf(
            "Referer",
            url,
        )

        videoList.add(
            StreamSource(videoUrl, "${prefix}Sibnet", headers = videoHeaders),
        )

        return videoList
    }

}
