package com.sf.tadami.animesources.extractors

import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import dev.datlag.jsunpacker.JsUnpacker
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class FilemoonExtractor(private val client: OkHttpClient) {

    fun videosFromUrl(url: String, prefix: String = "Filemoon - ", headers: Headers? = null): List<StreamSource> {
        return runCatching {
            val doc = client.newCall(GET(url)).execute().asJsoup()
            val jsEval = doc.selectFirst("script:containsData(eval):containsData(m3u8)")!!.data()
            val unpacked = JsUnpacker.unpackAndCombine(jsEval).orEmpty()
            val masterUrl = unpacked.takeIf(String::isNotBlank)
                ?.substringAfter("{file:\"", "")
                ?.substringBefore("\"}", "")
                ?.takeIf(String::isNotBlank)
                ?: return emptyList()

            val masterPlaylist = client.newCall(GET(masterUrl)).execute().body.string()

            val httpUrl = url.toHttpUrl()
            val videoHeaders = (headers?.newBuilder() ?: Headers.Builder())
                .set("Referer", url)
                .set("Origin", "https://${httpUrl.host}")
                .build()

            val separator = "#EXT-X-STREAM-INF:"
            masterPlaylist.substringAfter(separator).split(separator).map {
                val resolution = it.substringAfter("RESOLUTION=")
                    .substringAfter("x")
                    .substringBefore(",") + "p"
                val videoUrl = it.substringAfter("\n").substringBefore("\n")

                StreamSource(
                    videoUrl,
                    prefix + resolution,
                    headers = videoHeaders
                )
            }
        }.getOrElse { emptyList() }
    }
}