package com.sf.tadami.animesources.sources.fr.animesama.extractors

import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.network.GET
import com.sf.tadami.network.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient

class SendVidExtractor(private val client : OkHttpClient) {
    fun getVideoFromUrl(url: String, headers: Headers): List<StreamSource> {

        try{
            val videoList = mutableListOf<StreamSource>()
            val document = client.newCall(GET(url, headers = headers)).execute().asJsoup()

            val masterUrl = document.selectFirst("meta[property=og:video:secure_url]")?.attr("content") ?: return emptyList()
            val separator = "#EXT-X-STREAM-INF:"

            val masterPlaylist = client.newCall(GET(masterUrl)).execute().body.string()
            if (masterPlaylist.contains(separator)) {
                masterPlaylist.substringAfter(separator).substringBefore("#EXT-X-I-FRAME-STREAM-INF:")
                    .split(separator).forEach {
                        val quality = it.substringAfter("RESOLUTION=").substringAfter("x").substringBefore(",").substringBefore("\n") + "p"
                        var videoUrl = it.substringAfter("\n").substringBefore("\n")
                        if (!videoUrl.startsWith("http")) {
                            videoUrl = masterUrl.substringBeforeLast("/") + "/$videoUrl"
                        }
                        videoList.add(StreamSource(videoUrl, "SendVid: $quality"))
                    }
            } else {
                videoList.add(StreamSource(masterUrl, "SendVid"))
            }
            return videoList
        }
        catch (e : Exception){
            return emptyList()
        }
    }
}