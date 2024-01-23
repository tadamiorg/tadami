package com.sf.tadami.animesources.sources.fr.vostfree.extractors

import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.network.GET
import com.sf.tadami.network.asJsoup
import okhttp3.OkHttpClient

class MyViExtractor(private val client: OkHttpClient) {
    fun videosFromUrl(url: String): List<StreamSource> {
        val document = client.newCall(GET(url)).execute().asJsoup()
        val videoList = mutableListOf<StreamSource>()
        document.select("script").forEach { script ->
            if (script.data().contains("CreatePlayer(\"v")) {
                val videosString = script.data().toString()
                var videoUrl = videosString.substringAfter("\"v=").substringBefore("\\u0026tp=video").replace("%26", "&").replace("%3a", ":").replace("%2f", "/").replace("%3f", "?").replace("%3d", "=")
                if (!videoUrl.contains("https:")) {
                    videoUrl = "https:$videoUrl"
                    videoList.add(StreamSource(videoUrl, "Stream"))
                } else {
                    videoList.add(StreamSource(videoUrl, "MyVi"))
                }
            }
        }
        return videoList
    }
}