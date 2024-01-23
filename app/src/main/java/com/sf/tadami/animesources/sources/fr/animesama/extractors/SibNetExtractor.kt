package com.sf.tadami.animesources.sources.fr.animesama.extractors

import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.network.GET
import com.sf.tadami.network.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient

class SibNetExtractor(private val client : OkHttpClient) {
    private val baseURL : String = "https://video.sibnet.ru"

    fun getVideoFromUrl(url: String, headers: Headers): List<StreamSource> {
        try{
            val document = client.newCall(GET(url, headers = headers)).execute().asJsoup()
            val sibId = url.substringAfter("?videoid=").substringBefore("&")
            val regex = Regex("""player\.src\(\[.*\{src:\s*\"([^"]+${sibId}\.mp4)\",\s*type:\s*\"video/mp4\"\}.*\]\)""")
            val scriptTags = document.select("script")
            var playerPartialUrl : String? = null


            scriptTags.find {
                val data = it.data()
                val matchResult = regex.find(data ?: "")
                val group = matchResult?.groupValues?.getOrNull(1)
                if(group!=null){
                    playerPartialUrl = group
                }
                group != null
            }
            playerPartialUrl?.let{
                val newHeaders = headers.newBuilder().add("Referer",url).build()
                return listOf(StreamSource(baseURL + it,"Sibnet",newHeaders))
            } ?: return emptyList()
        }catch (e : Exception){
            return emptyList()
        }





    }
}