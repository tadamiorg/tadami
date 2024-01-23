package com.sf.tadami.animesources.extractors

import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.network.GET
import com.sf.tadami.network.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient

class YourUploadExtractor(private val client: OkHttpClient) {
    fun videosFromUrl(url: String, headers: Headers, name: String = "YourUpload", prefix: String = ""): List<StreamSource> {
        val newHeaders = headers.newBuilder().add("referer", "https://www.yourupload.com/").build()
        return runCatching {
            val request = client.newCall(GET(url, headers = newHeaders)).execute()
            val document = request.asJsoup()
            val baseData = document.selectFirst("script:containsData(jwplayerOptions)")!!.data()
            if (!baseData.isNullOrEmpty()) {
                val basicUrl = baseData.substringAfter("file: '").substringBefore("',")
                val quality = prefix + name
                listOf(StreamSource(basicUrl, quality, headers = newHeaders))
            } else {
                null
            }
        }.getOrNull() ?: emptyList()
    }
}