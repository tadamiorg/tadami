package com.sf.tadami.animesources.sources.en.animedao.extractors

import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import dev.datlag.jsunpacker.JsUnpacker
import okhttp3.Headers
import okhttp3.OkHttpClient

class MixDropExtractor(private val client: OkHttpClient) {
    fun videoFromUrl(url: String, lang: String = ""): List<StreamSource> {
        val doc = client.newCall(GET(url)).execute().asJsoup()
        val unpacked = doc.selectFirst("script:containsData(eval):containsData(MDCore)")
            ?.data()
            ?.let { JsUnpacker.unpackAndCombine(it) }
            ?: return emptyList()
        val videoUrl = "https:" + unpacked.substringAfter("Core.wurl=\"")
            .substringBefore("\"")
        val quality = ("MixDrop").let {
            if (lang.isNotBlank()) {
                "$it($lang)"
            } else {
                it
            }
        }
        val referer = Headers.headersOf("Referer", "https://mixdrop.co/")
        return listOf(StreamSource(videoUrl, quality, headers = referer))
    }
}