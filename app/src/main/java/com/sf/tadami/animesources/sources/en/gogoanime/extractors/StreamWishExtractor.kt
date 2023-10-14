package com.sf.tadami.animesources.sources.en.gogoanime.extractors

import com.sf.tadami.animesources.extractors.utils.PlaylistUtils
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import dev.datlag.jsunpacker.JsUnpacker
import okhttp3.Headers
import okhttp3.OkHttpClient

class StreamWishExtractor(private val client: OkHttpClient, private val headers: Headers)  {
    fun videosFromUrl(url: String, videoNameGen: (String) -> String = { quality -> "StreamWish - $quality" }): List<StreamSource> {
        val doc = client.newCall(GET(url, headers = headers)).execute().asJsoup()
        val jsEval = doc.selectFirst("script:containsData(m3u8)")?.data() ?: return emptyList()

        val masterUrl = JsUnpacker.unpackAndCombine(jsEval)
            ?.substringAfter("source")
            ?.substringAfter("file:\"")
            ?.substringBefore("\"")
            ?: return emptyList()

        return PlaylistUtils(client, headers).extractFromHls(masterUrl, videoNameGen = videoNameGen)
    }
}