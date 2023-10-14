package com.sf.tadami.animesources.extractors

import com.sf.tadami.animesources.extractors.unpacker.Unpacker
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient

class MixDropExtractor(private val client: OkHttpClient) {
    fun videoFromUrl(
        url: String,
        lang: String = "",
        prefix: String = ""
    ): List<StreamSource> {
        val doc = client.newCall(GET(url)).execute().asJsoup()
        val unpacked = doc.selectFirst("script:containsData(eval):containsData(MDCore)")
            ?.data()
            ?.let(Unpacker::unpack)
            ?: return emptyList()

        val videoUrl = "https:" + unpacked.substringAfter("Core.wurl=\"")
            .substringBefore("\"")

        val quality = prefix + ("MixDrop").let {
            when {
                lang.isNotBlank() -> "$it($lang)"
                else -> it
            }
        }

        val headers = Headers.headersOf("Referer", "https://mixdrop.co/")
        return listOf(StreamSource(videoUrl, quality, headers = headers))
    }
}
