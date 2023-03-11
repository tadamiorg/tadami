package com.sf.animescraper.animesources.extractors

import com.sf.animescraper.network.requests.okhttp.GET
import com.sf.animescraper.network.requests.utils.asJsoup
import com.sf.animescraper.network.api.model.StreamSource
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