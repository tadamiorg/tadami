package com.sf.tadami.animesources.sources.fr.animesama.extractors

import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient

class VkExtractor(private val client: OkHttpClient) {

    companion object {
        const val mobileUA: String =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
    }

    fun getVideosFromUrl(url: String): List<StreamSource> {
        try {
            val newHeaders = Headers.Builder().add("User-Agent", mobileUA).build()
            val document = client.newCall(GET(url, headers = newHeaders)).execute().asJsoup()
            val regex = Regex(""""url(\d{3,4})":"([^"]+)"""")
            val scriptTags = document.select("script")
            val sources = mutableListOf<StreamSource>()
            scriptTags.find { script ->
                val data = script.data()
                var dataFound = false
                val matchResult = regex.findAll(data ?: "")
                matchResult.forEach { result ->
                    val quality = result.groupValues.getOrNull(1)
                    val newUrlResult = result.groupValues.getOrNull(2)
                    if (quality != null && newUrlResult != null && quality.toInt() > 360) {
                        dataFound = true
                        val newUrl = newUrlResult.replace("\\/", "/")
                        sources.add(
                            StreamSource(
                                newUrl,
                                "Vk: ${quality}p",
                                newHeaders
                            )
                        )
                    }
                }
                dataFound
            }
            return sources
        } catch (e: Exception) {
            return emptyList()
        }
    }
}