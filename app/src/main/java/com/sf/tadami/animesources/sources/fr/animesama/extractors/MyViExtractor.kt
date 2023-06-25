package com.sf.tadami.animesources.sources.fr.animesama.extractors

import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.net.URLDecoder

class MyViExtractor(private val client : OkHttpClient) {
    fun getVideoFromUrl(url: String, headers: Headers): List<StreamSource> {
        try{
            val document = client.newCall(GET(url, headers = headers)).execute().asJsoup()
            val regex = Regex("""PlayerLoader\.CreatePlayer\s?\(\s?(?:\"|\')([^"']+)""")
            val scriptTags = document.select("script")
            var encodedUrlParameters : String? = null

            scriptTags.find {
                val data = it.data()
                val matchResult = regex.find(data ?: "")
                val group = matchResult?.groupValues?.getOrNull(1)
                if(group!=null){
                    encodedUrlParameters = group
                }
                group != null
            }
            encodedUrlParameters?.let{
                val encodedUrl = it.substringAfter("v=").substringBefore("\\u0026")
                val decodedUrl = URLDecoder.decode(encodedUrl,"UTF-8")
                val response = client.newBuilder().followRedirects(false).build().newCall(GET(decodedUrl, headers = headers)).execute()
                response.use { res ->
                    // Process the response
                    if (res.code == 302 && res.header("Location") != null) {
                        val newURL = res.header("Location")
                        return listOf(StreamSource(newURL!!, "MyVi"))
                    } else {
                        return listOf(StreamSource(decodedUrl, "MyVi"))
                    }
                }
            } ?: return emptyList()
        }catch (e : Exception){
            return emptyList()
        }
    }
}