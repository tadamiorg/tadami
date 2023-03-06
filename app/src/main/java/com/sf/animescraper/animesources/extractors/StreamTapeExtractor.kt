package com.sf.animescraper.animesources.extractors

import com.sf.animescraper.network.requests.okhttp.GET
import com.sf.animescraper.network.requests.utils.asJsoup
import com.sf.animescraper.network.scraping.dto.crypto.StreamSource
import okhttp3.OkHttpClient

class StreamTapeExtractor(private val client: OkHttpClient) {
    fun videoFromUrl(url: String, quality: String = "StreamTape"): StreamSource? {
        val baseUrl = "https://streamtape.com/e/"
        val newUrl = if (!url.startsWith(baseUrl)) {
            // ["https", "", "<domain>", "<???>", "<id>", ...]
            val id = runCatching { url.split("/").get(4) }.getOrNull() ?: return null
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
        return StreamSource(videoUrl, quality)
    }
}