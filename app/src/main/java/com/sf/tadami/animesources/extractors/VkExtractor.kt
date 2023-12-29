package com.sf.tadami.animesources.extractors

import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class VkExtractor(private val client: OkHttpClient, private val headers: Headers) {

    private val documentHeaders by lazy {
        headers.newBuilder()
            .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .build()
    }

    private fun getVideoHeaders(videoUrl : String) : Headers{
        return  headers.newBuilder()
            .add("Accept", "*/*")
            .add("Host", videoUrl.toHttpUrl().host)
            .add("Origin", VK_URL)
            .add("Referer", "$VK_URL/")
            .build()
    }
    fun videosFromUrl(url: String, prefix: String = ""): List<StreamSource> {
        val data = client.newCall(
            GET(url, headers = documentHeaders),
        ).execute().body.string()

        return REGEX_VIDEO.findAll(data).map {
            val quality = it.groupValues[1]
            val videoUrl = it.groupValues[2].replace("\\/", "/")
            StreamSource(videoUrl, "$prefix Vk: ${quality}p", headers = getVideoHeaders(videoUrl))
        }.toList()
    }
    companion object {
        private const val VK_URL = "https://vk.com"
        private val REGEX_VIDEO = """"url(\d+)":"(.*?)"""".toRegex()
    }
}
