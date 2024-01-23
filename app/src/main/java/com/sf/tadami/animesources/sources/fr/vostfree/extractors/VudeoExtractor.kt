package com.sf.tadami.animesources.sources.fr.vostfree.extractors

import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.network.GET
import com.sf.tadami.network.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient

class VudeoExtractor(private val client: OkHttpClient) {
    fun videosFromUrl(url: String, headers: Headers): List<StreamSource> {
        val document = client.newCall(GET(url)).execute().asJsoup()
        val videoList = mutableListOf<StreamSource>()
        document.select("script:containsData(sources: [)").forEach { script ->
            val videoUrl = script.data().substringAfter("sources: [").substringBefore("]").replace("\"", "").split(",")
            videoUrl.forEach {
                videoList.add(StreamSource(it, "Vudeo", headers))
            }
        }
        return videoList
    }
}