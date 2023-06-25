package com.sf.tadami.animesources.extractors

import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import com.sf.tadami.network.api.model.StreamSource
import okhttp3.OkHttpClient

class StreamTapeExtractor(private val client: OkHttpClient) {
    fun videoFromUrl(url: String, quality: String = "StreamTape"): StreamSource? {
        val baseUrl = "https://streamtape.com/e/"
        val newUrl = if (!url.startsWith(baseUrl)) {
            // ["https", "", "<domain>", "<???>", "<id>", ...]
            val id = url.split("/").getOrNull(4) ?: return null
            baseUrl + id
        } else { url }
        val document = client.newCall(GET(newUrl)).execute().asJsoup()
        val targetLine = "document.getElementById('robotlink')"
        val script = document.selectFirst("script:containsData($targetLine)")
            ?.data()
            ?.substringAfter("$targetLine.innerHTML = '")
            ?: return null
        val videoUrl = "https:" + script.substringBefore("'") +
                script.substringAfter("+ ('xcd").substringBefore("'")
        val response = client.newBuilder().followRedirects(false).build().newCall(GET(videoUrl)).execute()
        response.use { res ->
            // Process the response
            if (res.code == 302 && res.header("Location") != null) {
                val newURL = res.header("Location")
                return StreamSource(newURL!!, quality)
            } else {
                return StreamSource(videoUrl, quality)
            }
        }
    }
}