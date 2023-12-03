package com.sf.tadami.animesources.extractors.unused.streamsbextractor

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.animesources.extractors.ExtractorsPreferences
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.utils.editPreferences
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class StreamSBExtractor(private val client: OkHttpClient) {

    private val dataStore: DataStore<Preferences> = Injekt.get()
    private var extractorsPreferences: ExtractorsPreferences = runBlocking {
        dataStore.getPreferencesGroup(ExtractorsPreferences)
    }

    companion object {
        private const val ENDPOINT_URL =
            "https://raw.githubusercontent.com/Claudemirovsky/streamsb-endpoint/master/endpoint.txt"
    }

    private val json: Json by injectLazy()

    private fun getEndpoint() = extractorsPreferences.streamSbEndpoint

    private fun updateEndpoint() {
        client.newCall(GET(ENDPOINT_URL)).execute()
            .use { it.body.string() }
            .let {
                runBlocking {
                    dataStore.editPreferences(extractorsPreferences.copy(streamSbEndpoint = it),ExtractorsPreferences) {
                        extractorsPreferences = it
                    }
                }
            }
    }

    private fun bytesToHex(bytes: ByteArray): String {
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
        val sbUrl = "https://$host" + getEndpoint()
        val id = url.substringAfter(host)
            .substringAfter("/e/")
            .substringAfter("/embed-")
            .substringBefore("?")
            .substringBefore(".html")
            .substringAfter("/")
        return sbUrl + if (common) {
            val hexBytes = bytesToHex(id.toByteArray())
            "/625a364258615242766475327c7c${hexBytes}7c7c4761574550654f7461566d347c7c73747265616d7362"
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
                .set("referer", trimmedUrl)
                .set("watchsb", "sbstream")
                .set("authority", "embedsb.com")
                .build()
        }
        return runCatching {
            val master = if (manualData) trimmedUrl else fixUrl(trimmedUrl, common)
            val request = client.newCall(GET(master, newHeaders)).execute()

            val json = json.decodeFromString<Response>(
                if (request.code == 200) {
                    request.use { it.body.string() }
                } else {
                    request.close()
                    updateEndpoint()
                    client.newCall(GET(fixUrl(trimmedUrl, common), newHeaders))
                        .execute()
                        .use { it.body.string() }
                },
            )

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
                val quality = ("StreamSB: $resolution").let {
                    buildString {
                        if (prefix.isNotBlank()) append("$prefix ")
                        append(it)
                        if (prefix.isNotBlank()) append(" $suffix")
                    }
                }
                val videoUrl = it.substringAfter("\n").substringBefore("\n")
                StreamSource(videoUrl, quality, headers = newHeaders)
            }
        }.getOrNull() ?: emptyList()
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