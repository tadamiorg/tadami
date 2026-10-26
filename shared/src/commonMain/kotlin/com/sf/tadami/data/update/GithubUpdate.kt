package com.sf.tadami.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val APK_CONTENT_TYPE = "application/vnd.android.package-archive"

@Serializable
data class GithubUpdate(
    @SerialName("tag_name") val version: String,
    // GitHub returns null for a release published without notes
    @SerialName("body") private val body: String? = null,
    @SerialName("html_url") val releaseLink: String,
    @SerialName("assets") private val assets: List<Assets> = emptyList()
){
    val info: String
        get() = body.orEmpty()

    /**
     * The first APK of the release, or null when it has none (e.g. published before its upload finished).
     */
    fun getDownloadLink(): String? {
        return assets.firstOrNull { it.isApk() }?.downloadLink
    }

    @Serializable
    data class Assets(
        @SerialName("name") val name: String = "",
        @SerialName("content_type") val contentType: String? = null,
        @SerialName("browser_download_url") val downloadLink: String
    ) {
        fun isApk(): Boolean {
            return name.endsWith(".apk", ignoreCase = true) || contentType == APK_CONTENT_TYPE
        }
    }
}
