package com.sf.tadami.animesources.sources.en.gogoanime.extractors

import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import dev.datlag.jsunpacker.JsUnpacker
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.jsoup.Jsoup

class Mp4uploadExtractor(private val client : OkHttpClient) {
    fun getVideoFromUrl(url: String, headers: Headers, prefix: String = ""): List<StreamSource> {
        val body = client.newCall(GET(url, headers = headers)).execute().body.string()

        val videoUrl = if (body.contains("eval(function(p,a,c,k,e,d)")) {
            val packed = body.substringAfter("<script type='text/javascript'>eval(function(p,a,c,k,e,d)")
                .substringBefore("</script>")
            body.substringAfter("<script type='text/javascript'>eval(function(p,a,c,k,e,d)")
                .substringBefore("</script>")
            val unpacked = JsUnpacker.unpackAndCombine("eval(function(p,a,c,k,e,d)$packed") ?: return emptyList()
            unpacked.substringAfter("player.src(\"").substringBefore("\");")
        } else {
            val script = Jsoup.parse(body).selectFirst("script:containsData(player.src)")?.data() ?: return emptyList()
            script.substringAfter("src: \"").substringBefore("\"")
        }

        return listOf(
            StreamSource(videoUrl, "${prefix}Mp4upload",headers = Headers.headersOf("Referer", "https://www.mp4upload.com/")),
        )
    }
}