package com.sf.tadami.animesources.extractors

import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import okhttp3.OkHttpClient

class MytvExtractor(private val client: OkHttpClient) {

    fun videosFromUrl(url: String, prefix: String = ""): List<StreamSource> {
        val document = client.newCall(GET(url)).execute().asJsoup()
        val videoList = mutableListOf<StreamSource>()
        document.select("script").forEach { script ->
            if (script.data().contains("CreatePlayer(\"v")) {
                val videosString = script.data().toString()
                val videoUrl = videosString.substringAfter("\"v=").substringBefore("\\u0026tp=video").replace("%26", "&").replace("%3a", ":").replace("%2f", "/").replace("%3f", "?").replace("%3d", "=")
                if (!videoUrl.contains("https:")) {
                    val videoUrl = "https:$videoUrl"
                    videoList.add(StreamSource(videoUrl, "${prefix}Stream"))
                } else {
                    videoList.add(StreamSource(videoUrl, "${prefix}Mytv"))
                }
            }
        }
        return videoList
    }

}
