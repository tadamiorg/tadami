package com.sf.tadami.domain.anime

import com.sf.tadami.source.model.SAnimeStatus

/**
 * An anime of the app, as the app passes it to the sources. Extensions only read it: the app's class
 * has more members (it's a data class), which aren't part of the extension API.
 */
class Anime private constructor() {
    val id: Long get() = throw Exception("Stub!")
    val source: Long get() = throw Exception("Stub!")
    val url: String get() = throw Exception("Stub!")
    val title: String get() = throw Exception("Stub!")
    val rawTitle: String? get() = throw Exception("Stub!")
    val language: String? get() = throw Exception("Stub!")
    val thumbnailUrl: String? get() = throw Exception("Stub!")
    val release: String? get() = throw Exception("Stub!")
    val studio: String? get() = throw Exception("Stub!")
    val author: String? get() = throw Exception("Stub!")
    val status: SAnimeStatus get() = throw Exception("Stub!")
    val description: String? get() = throw Exception("Stub!")
    val genres: List<String>? get() = throw Exception("Stub!")
    val favorite: Boolean get() = throw Exception("Stub!")
    val lastUpdate: Long get() = throw Exception("Stub!")
    val nextUpdate: Long get() = throw Exception("Stub!")
    val fetchInterval: Int get() = throw Exception("Stub!")
    val initialized: Boolean get() = throw Exception("Stub!")
    val episodeFlags: Long get() = throw Exception("Stub!")
    val dateAdded: Long get() = throw Exception("Stub!")
}
