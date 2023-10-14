package com.sf.tadami.animesources.extractors

import android.util.Base64
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.utils.asJsoup
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class ChillxExtractor(private val client: OkHttpClient, private val headers: Headers) {

    fun videoFromUrl(url: String, referer: String, prefix: String = "Chillx - "): List<StreamSource> {
        val videoList = mutableListOf<StreamSource>()
        val mainUrl = "https://${url.toHttpUrl().host}"

        val document = client.newCall(
            GET(url, headers = Headers.headersOf("Referer", "$referer/"))
        ).execute().asJsoup().html()

        val master = Regex("""MasterJS\s*=\s*'([^']+)""").find(document)?.groupValues?.get(1)
        val aesJson = Json.decodeFromString<CryptoInfo>(base64Decode(master ?: return emptyList()).toString(Charsets.UTF_8))

        val decrypt = cryptoAESHandler(aesJson, KEY)

        val masterUrl = Regex("""sources:\s*\[\{"file":"([^"]+)""").find(decrypt)?.groupValues?.get(1)
            ?: Regex("""file: ?"([^"]+)"""").find(decrypt)?.groupValues?.get(1)
            ?: return emptyList()

        val masterHeaders = Headers.headersOf(
            "Accept", "*/*",
            "Connection", "keep-alive",
            "Sec-Fetch-Dest", "empty",
            "Sec-Fetch-Mode", "cors",
            "Sec-Fetch-Site", "cross-site",
            "Origin", mainUrl,
            "Referer", "$mainUrl/",
        )

        val response = client.newCall(GET(masterUrl, headers = masterHeaders)).execute()

        val masterPlaylist = response.body.string()
        val masterBase = "https://${masterUrl.toHttpUrl().host}${masterUrl.toHttpUrl().encodedPath}"
            .substringBeforeLast("/") + "/"

        masterPlaylist.substringAfter("#EXT-X-STREAM-INF:")
            .split("#EXT-X-STREAM-INF:").map {
                val quality = it.substringAfter("RESOLUTION=").split(",")[0].split("\n")[0].substringAfter("x") + "p"

                var videoUrl = it.substringAfter("\n").substringBefore("\n")
                if (videoUrl.startsWith("https").not()) {
                    videoUrl = masterBase + videoUrl
                }
                val videoHeaders = headers.newBuilder()
                    .addAll(masterHeaders)
                    .build()


                videoList.add(StreamSource(videoUrl, prefix + quality, headers = videoHeaders))

            }
        return videoList
    }

    private fun cryptoAESHandler(
        data: CryptoInfo,
        pass: String,
    ): String {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        val spec = PBEKeySpec(
            pass.toCharArray(),
            data.salt?.hexToByteArray(),
            data.iterations ?: 1,
            256
        )
        val key = factory.generateSecret(spec)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key.encoded, "AES"),
            IvParameterSpec(data.iv?.hexToByteArray())
        )
        return String(cipher.doFinal(base64Decode(data.ciphertext.toString())))
    }

    private fun base64Decode(string: String): ByteArray {
        return Base64.decode(string, Base64.DEFAULT)
    }

    @Serializable
    data class CryptoInfo(
        val ciphertext: String? = null,
        val iv: String? = null,
        val salt: String? = null,
        val iterations: Int? = null,
    )

    private fun String.hexToByteArray(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }
        return chunked(2)
            .map { it.toInt(16).toByte() }

            .toByteArray()
    }

    companion object {
        private const val KEY = "11x&W5UBrcqn\$9Yl"
    }
}