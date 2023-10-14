package com.sf.tadami.animesources.sources.en.gogoanime.extractors

import android.util.Base64
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
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

class GogoCdnExtractor(private val client: OkHttpClient,private val json : Json) {
    fun videosFromUrl(serverUrl: String): List<StreamSource> {
        try {
            val document = client.newCall(GET(serverUrl)).execute().asJsoup()
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

            val httpUrl = serverUrl.toHttpUrl()
            val host = "https://" + httpUrl.host + "/"
            val id = httpUrl.queryParameter("id") ?: throw Exception("error getting id")
            val encryptedId = cryptoHandler(id, iv, secretKey)
            val token = httpUrl.queryParameter("token")
            val qualityPrefix = if (token != null) "Gogostream - " else "Vidstreaming - "

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

                val separator = "#EXT-X-STREAM-INF:"

                val masterPlaylist = client.newCall(GET(fileURL)).execute().body.string()
                if (masterPlaylist.contains(separator)) {
                    masterPlaylist.substringAfter(separator)
                        .split(separator).forEach {
                            val quality = it.substringAfter("RESOLUTION=").substringAfter("x").substringBefore(",").substringBefore("\n") + "p"
                            var videoUrl = it.substringAfter("\n").substringBefore("\n")
                            if (!videoUrl.startsWith("http")) {
                                videoUrl = fileURL.substringBeforeLast("/") + "/$videoUrl"
                            }
                            videoList.add(StreamSource(videoUrl, qualityPrefix + quality))
                        }
                } else {
                    videoList.add(StreamSource(fileURL, "${qualityPrefix}Original"))
                }
            } else {
                array.forEach {
                    val label = it.jsonObject["label"].toString().lowercase(Locale.ROOT)
                        .trim('"').replace(" ", "")
                    val fileURL = it.jsonObject["file"].toString().trim('"')
                    val videoHeaders = Headers.headersOf("Referer", serverUrl)
                    if (label == "auto") {
                        autoList.add(
                            StreamSource(
                                fileURL,
                                qualityPrefix + label,
                                headers = videoHeaders,
                            ),
                        )
                    } else {
                        videoList.add(StreamSource(fileURL, qualityPrefix + label, headers = videoHeaders))
                    }
                }
            }
            return videoList.sortedByDescending {
                it.quality.substringAfter(qualityPrefix).substringBefore("p").toIntOrNull() ?: -1
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