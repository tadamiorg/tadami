package com.sf.tadami.animesources.extractors

import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import com.sf.tadami.network.api.model.StreamSource
import okhttp3.OkHttpClient

class VoeExtractor(private val client: OkHttpClient) {
    fun videoFromUrl(url: String, quality: String? = null): StreamSource? {
        val document = client.newCall(GET(url)).execute().asJsoup()
        val script = document.selectFirst("script:containsData(const sources),script:containsData(var sources)")
            ?.data()
            ?: return null
        val videoUrl = script.substringAfter("hls': '").substringBefore("'")
        val resolution = script.substringAfter("video_height': ").substringBefore(",")
        val qualityStr = quality ?: "VoeCDN(${resolution}p)"
        return StreamSource(videoUrl, qualityStr)
    }
}