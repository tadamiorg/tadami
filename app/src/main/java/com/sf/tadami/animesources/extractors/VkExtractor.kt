package com.sf.tadami.animesources.extractors

import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class VkExtractor(private val client: OkHttpClient, private val headers: Headers) {
    fun videosFromUrl(url: String, prefix: String = ""): List<StreamSource> {
        val documentHeaders = headers.newBuilder()
            .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .add("Host", "vk.com")
            .build()

        val data = client.newCall(
            GET(url, headers = documentHeaders),
        ).execute().body.string()

        val videoRegex = """"url(\d+)":"(.*?)"""".toRegex()
        return videoRegex.findAll(data).map {
            val quality = it.groupValues[1]
            val videoUrl = it.groupValues[2].replace("\\/", "/")
            val videoHeaders = headers.newBuilder()
                .add("Accept", "*/*")
                .add("Host", videoUrl.toHttpUrl().host)
                .add("Origin", "https://vk.com")
                .add("Referer", "https://vk.com/")
                .build()
            StreamSource(videoUrl, "Vk: ${quality}p", headers = videoHeaders)
        }.toList()
    }
}
