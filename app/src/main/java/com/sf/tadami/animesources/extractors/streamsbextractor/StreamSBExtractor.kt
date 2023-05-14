package com.sf.tadami.animesources.extractors.streamsbextractor

import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.api.online.AnimeSourceBase
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.okhttp.parseAs
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import kotlin.random.Random

class StreamSBExtractor(private val client: OkHttpClient) {

    private fun rdHexaStr(size: Int): String {
        require(size > 0) { "Size must be greater than zero." }

        val charPool = "0123456789ABCDEF"
        val random = Random.Default

        val stringBuilder = StringBuilder(size)
        repeat(size) {
            val randomIndex = random.nextInt(charPool.length)
            val randomChar = charPool[randomIndex]
            stringBuilder.append(randomChar)
        }

        return stringBuilder.toString()
    }

    protected fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789ABCDEF".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF

            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    // animension, asianload and dramacool uses "common = false"
    private fun fixUrl(url: String, common: Boolean): String {
        val host = url.toHttpUrl().host
        val sbUrl = "https://$host"
        val id = url.substringAfter(host)
            .substringAfter("/e/")
            .substringAfter("/embed-")
            .substringBefore("?")
            .substringBefore(".html")
            .substringAfter("/")
        return sbUrl + if (common) {
            val hexBytes = bytesToHex(id.toByteArray())
            "/375664356a494546326c4b797c7c6e756577776778623171737/${rdHexaStr(24)}7c7c${hexBytes}7c7c${rdHexaStr(24)}7c7c73747265616d7362"
        } else {
            "/${bytesToHex("||$id||||streamsb".toByteArray())}/"
        }
    }

    fun videosFromUrl(
        url: String,
        headers: Headers,
        prefix: String = "",
        suffix: String = "",
        common: Boolean = true,
        manualData: Boolean = false,
    ): List<StreamSource> {
        val trimmedUrl = url.trim() // Prevents some crashes
        val newHeaders = if (manualData) {
            headers
        } else {
            headers.newBuilder()
                .set("Host", trimmedUrl.toHttpUrl().host)
                .set("User-Agent", AnimeSourceBase.DEFAULT_USER_AGENT)
                .set("watchsb", "sbstream")
                .build()
        }
        return try {
            run {
                val master = if (manualData) trimmedUrl else fixUrl(trimmedUrl, common)
                val request = client.newCall(GET(master, newHeaders)).execute()

                val json = if (request.code == 200) {
                    request.parseAs<Response>()
                } else {
                    request.close()
                    client.newCall(GET(fixUrl(trimmedUrl, common), newHeaders))
                        .execute().parseAs()

                }

                val masterUrl = json.stream_data.file.trim('"')

                val masterPlaylist = client.newCall(GET(masterUrl, newHeaders))
                    .execute()
                    .use { it.body.string() }


                val separator = "#EXT-X-STREAM-INF"
                masterPlaylist.substringAfter(separator).split(separator).map {
                    val resolution = it.substringAfter("RESOLUTION=")
                        .substringBefore("\n")
                        .substringAfter("x")
                        .substringBefore(",") + "p"
                    val quality = ("StreamSB:$resolution").let {
                        buildString {
                            if (prefix.isNotBlank()) append("$prefix ")
                            append(it)
                            if (prefix.isNotBlank()) append(" $suffix")
                        }
                    }
                    val videoUrl = it.substringAfter("\n").substringBefore("\n")
                    StreamSource(videoUrl, quality, headers = newHeaders)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun videosFromDecryptedUrl(
        realUrl: String,
        headers: Headers,
        prefix: String = "",
        suffix: String = ""
    ): List<StreamSource> {
        return videosFromUrl(realUrl, headers, prefix, suffix, manualData = true)
    }
}