package com.sf.tadami.desktop.extension

import kotlinx.serialization.Serializable

/** The official repo, the Android app's default one. */
const val DEFAULT_EXTENSIONS_REPO = "https://raw.githubusercontent.com/tadamiorg/tadami-extensions/main"

/** An extension of a repo's `index.min.json`. */
@Serializable
data class IndexEntry(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val sha256: String? = null,
    val size: Long? = null,
    val sources: List<IndexSource> = emptyList(),
) {
    val displayName: String get() = name.substringAfter("Tadami: ")

    fun apkUrl(repoUrl: String) = "$repoUrl/apk/$apk"

    fun iconUrl(repoUrl: String) = "$repoUrl/icon/$pkg.png"
}

@Serializable
data class IndexSource(
    val name: String,
    val lang: String,
    val id: String,
    val baseUrl: String,
)
