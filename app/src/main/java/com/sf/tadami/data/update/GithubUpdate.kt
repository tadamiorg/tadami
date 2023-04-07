package com.sf.tadami.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubUpdate(
    @SerialName("tag_name") val version: String,
    @SerialName("body") val info: String,
    @SerialName("html_url") val releaseLink: String,
    @SerialName("assets") private val assets: List<Assets>
){
    fun getDownloadLink(): String {
        return assets[0].downloadLink
    }

    @Serializable
    data class Assets(@SerialName("browser_download_url") val downloadLink: String)
}