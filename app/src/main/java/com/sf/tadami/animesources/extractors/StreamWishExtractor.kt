package com.sf.tadami.animesources.extractors

import com.sf.tadami.animesources.extractors.utils.PlaylistUtils
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import dev.datlag.jsunpacker.JsUnpacker
import okhttp3.Headers
import okhttp3.OkHttpClient

class StreamWishExtractor(private val client: OkHttpClient, private val headers: Headers) {
    private val playlistUtils by lazy { PlaylistUtils(client, headers) }

    fun videosFromUrl(url: String, prefix: String) = videosFromUrl(url) { "$prefix - $it" }

    fun videosFromUrl(url: String, videoNameGen: (String) -> String = { quality -> "StreamWish - $quality" }): List<StreamSource> {
        val doc = client.newCall(GET(url, headers)).execute()
            .use { it.asJsoup() }
        // Sometimes the script body is packed, sometimes it isn't
        val scriptBody = doc.selectFirst("script:containsData(m3u8)")?.data()
            ?.let { script ->
                if (script.contains("eval(function(p,a,c")) {
                    JsUnpacker.unpackAndCombine(script)
                } else script
            }

        val masterUrl = scriptBody
            ?.substringAfter("source", "")
            ?.substringAfter("file:\"", "")
            ?.substringBefore("\"", "")
            ?.takeIf(String::isNotBlank)
            ?: return emptyList()

        return playlistUtils.extractFromHls(masterUrl, url, videoNameGen = videoNameGen)
    }
}