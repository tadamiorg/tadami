package com.sf.tadami.animesources.extractors

import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient

class SendvidExtractor(private val client: OkHttpClient, private val headers: Headers) {
    fun videosFromUrl(url: String, prefix: String = ""): List<StreamSource> {
        val videoList = mutableListOf<StreamSource>()
        val document = client.newCall(GET(url)).execute().asJsoup()
        val videoUrl = document.selectFirst("source#video_source")?.attr("src") ?: return emptyList()

        videoList.add(StreamSource(videoUrl, "$prefix Sendvid"))

        return videoList
    }
}
