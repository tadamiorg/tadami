package com.sf.tadami.animesources.extractors

import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.network.GET
import com.sf.tadami.network.asJsoup
import dev.datlag.jsunpacker.JsUnpacker
import okhttp3.Headers
import okhttp3.OkHttpClient

class Mp4uploadExtractor(private val client: OkHttpClient) {
    fun videosFromUrl(url: String, headers: Headers, prefix: String = "", suffix: String = ""): List<StreamSource> {
        val newHeaders = headers.newBuilder()
            .add("referer", REFERER)
            .build()

        val doc = client.newCall(GET(url, newHeaders)).execute().use { it.asJsoup() }

        val script = doc.selectFirst("script:containsData(eval):containsData(p,a,c,k,e,d)")?.data()
            ?.let(JsUnpacker::unpackAndCombine)
            ?: doc.selectFirst("script:containsData(player.src)")?.data()
            ?: return emptyList()

        val videoUrl = script.substringAfter(".src(").substringBefore(")")
            .substringAfter("src:").substringAfter('"').substringBefore('"')

        val resolution = QUALITY_REGEX.find(script)?.groupValues?.let { "${it[1]}p" } ?: "Unknown resolution"
        val quality = "${prefix}Mp4Upload: $resolution$suffix"

        return listOf(StreamSource(videoUrl, quality, newHeaders))
    }

    companion object {
        private val QUALITY_REGEX by lazy { """\WHEIGHT=(\d+)""".toRegex() }
        private const val REFERER = "https://mp4upload.com/"
    }
}