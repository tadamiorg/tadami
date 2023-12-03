package com.sf.tadami.animesources.extractors

import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import okhttp3.OkHttpClient

class VoeExtractor(private val client: OkHttpClient) {
    fun videosFromUrl(url: String, quality: String? = null): List<StreamSource> {
        val document = client.newCall(GET(url)).execute().asJsoup()
        val script = document.selectFirst("script:containsData(const sources),script:containsData(var sources)")
            ?.data()
            ?: return emptyList()
        val videoUrl = script.substringAfter("hls': '").substringBefore("'")
        val resolution = script.substringAfter("video_height': ").substringBefore(",")
        val qualityStr = quality ?: "VoeCDN(${resolution}p)"
        return listOf(StreamSource(videoUrl, qualityStr))
    }
}