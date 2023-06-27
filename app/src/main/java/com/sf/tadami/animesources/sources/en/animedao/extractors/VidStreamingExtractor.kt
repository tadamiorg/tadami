package com.sf.tadami.animesources.sources.en.animedao.extractors

import android.util.Base64
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@ExperimentalSerializationApi
class VidstreamingExtractor(private val client: OkHttpClient, private val json: Json) {
    fun videosFromUrl(serverUrl: String, prefix: String = ""): List<StreamSource> {
        try {
            var document = client.newCall(GET(serverUrl)).execute().asJsoup()
            var newUrl = serverUrl
            if (serverUrl.contains("/embedded/") && document.selectFirst("body")!!.childrenSize() == 1) {
                newUrl = document.selectFirst("iframe")!!.attr("src")
                document = client.newCall(
                    GET(newUrl),
                ).execute().asJsoup()
            }

            val script = document.selectFirst("script:containsData(playerInstance)")
            if (script != null) {
                val url = script.data().substringAfter("\"file\": '").substringBefore("'")
                val headers = Headers.headersOf(
                    "Accept",
                    "*/*",
                    "Host",
                    url.toHttpUrl().host,
                    "Origin",
                    "https://${newUrl.toHttpUrl().host}",
                )

                val videoList = mutableListOf<StreamSource>()
                val masterPlaylist = client.newCall(GET(url, headers = headers)).execute().body.string()

                if (masterPlaylist.contains("#EXT-X-STREAM-INF:")) {
                    masterPlaylist.substringAfter("#EXT-X-STREAM-INF:")
                        .split("#EXT-X-STREAM-INF:").forEach {
                            val quality = it.substringAfter("RESOLUTION=").substringAfter("x").substringBefore(",").substringBefore("\n") + "p"
                            var videoUrl = it.substringAfter("\n").substringBefore("\n")
                            if (!videoUrl.startsWith("http")) {
                                videoUrl = url.substringBeforeLast("/") + "/$videoUrl"
                            }
                            videoList.add(StreamSource(videoUrl, "$prefix: $quality (VidStreaming)"))
                        }
                } else {
                    videoList.add(StreamSource(url, "$prefix (VidStreaming)"))
                }
                return videoList
            }
            val iv = document.select("div.wrapper")
                .attr("class").substringAfter("container-")
                .filter { it.isDigit() }.toByteArray()
            val secretKey = document.select("body[class]")
                .attr("class").substringAfter("container-")
                .filter { it.isDigit() }.toByteArray()
            val decryptionKey = document.select("div.videocontent")
                .attr("class").substringAfter("videocontent-")
                .filter { it.isDigit() }.toByteArray()
            val encryptAjaxParams = cryptoHandler(
                document.select("script[data-value]")
                    .attr("data-value"),
                iv,
                secretKey,
                false,
            ).substringAfter("&")

            val httpUrl = newUrl.toHttpUrl()
            val host = "https://" + httpUrl.host + "/"
            val id = httpUrl.queryParameter("id") ?: throw Exception("error getting id")
            val encryptedId = cryptoHandler(id, iv, secretKey)
            val token = httpUrl.queryParameter("token")
            val qualitySuffix = if (token != null) " (Gogostream)" else " (Vidstreaming)"

            val jsonResponse = client.newCall(
                GET(
                    "${host}encrypt-ajax.php?id=$encryptedId&$encryptAjaxParams&alias=$id",
                    Headers.headersOf(
                        "X-Requested-With",
                        "XMLHttpRequest",
                    ),
                ),
            ).execute().body.string()

            val data = json.parseToJsonElement(jsonResponse).jsonObject["data"]!!.jsonPrimitive.content
            val decryptedData = cryptoHandler(data, iv, decryptionKey, false)
            val videoList = mutableListOf<StreamSource>()
            val autoList = mutableListOf<StreamSource>()
            val array = json.parseToJsonElement(decryptedData).jsonObject["source"]!!.jsonArray
            if (array.size == 1 && array[0].jsonObject["type"]!!.jsonPrimitive.content == "hls") {
                val fileURL = array[0].jsonObject["file"].toString().trim('"')
                val masterPlaylist = client.newCall(GET(fileURL)).execute().body.string()
                masterPlaylist.substringAfter("#EXT-X-STREAM-INF:")
                    .split("#EXT-X-STREAM-INF:").forEach {
                        val quality = it.substringAfter("RESOLUTION=").substringAfter("x").substringBefore(",").substringBefore("\n") + "p"
                        var videoUrl = it.substringAfter("\n").substringBefore("\n")
                        if (!videoUrl.startsWith("http")) {
                            videoUrl = fileURL.substringBeforeLast("/") + "/$videoUrl"
                        }
                        videoList.add(StreamSource(videoUrl, "$prefix: $quality $qualitySuffix"))
                    }
            } else {
                array.forEach {
                    val label = it.jsonObject["label"].toString().lowercase(Locale.ROOT)
                        .trim('"').replace(" ", "")
                    val fileURL = it.jsonObject["file"].toString().trim('"')
                    val videoHeaders = Headers.headersOf("Referer", newUrl)
                    if (label == "auto") {
                        autoList.add(
                            StreamSource(
                                fileURL,
                                "$prefix: $label $qualitySuffix",
                                headers = videoHeaders,
                            ),
                        )
                    } else {
                        videoList.add(StreamSource(fileURL, "$prefix: $label $qualitySuffix", headers = videoHeaders))
                    }
                }
            }
            return videoList.sortedByDescending {
                it.quality.substringBefore(qualitySuffix).substringBefore("p").toIntOrNull() ?: -1
            } + autoList
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun cryptoHandler(
        string: String,
        iv: ByteArray,
        secretKeyString: ByteArray,
        encrypt: Boolean = true,
    ): String {
        val ivParameterSpec = IvParameterSpec(iv)
        val secretKey = SecretKeySpec(secretKeyString, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        return if (!encrypt) {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
            String(cipher.doFinal(Base64.decode(string, Base64.DEFAULT)))
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
            Base64.encodeToString(cipher.doFinal(string.toByteArray()), Base64.NO_WRAP)
        }
    }
}