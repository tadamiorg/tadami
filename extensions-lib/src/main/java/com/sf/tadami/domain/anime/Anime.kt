package com.sf.tadami.domain.anime

import com.sf.tadami.source.model.SAnimeStatus

/**
 * An anime of the app, as the app passes it to the Rx fetches of the sources. Extensions only read
 * it: the app's class has more members (it's a data class), which aren't part of the extension API.
 * The suspend methods of lib 1.7 take an [com.sf.tadami.source.model.SAnime] instead.
 *
 * @since extensions-lib 1.5
 */
class Anime private constructor() {

    /**
     * Its ID in the app's database.
     *
     * @since extensions-lib 1.5
     */
    val id: Long get() = throw Exception("Stub!")

    /**
     * The ID of its source.
     *
     * @since extensions-lib 1.5
     */
    val source: Long get() = throw Exception("Stub!")

    /**
     * Its URL, as the source set it in [com.sf.tadami.source.model.SAnime.url].
     *
     * @since extensions-lib 1.5
     */
    val url: String get() = throw Exception("Stub!")

    /**
     * Its title.
     *
     * @since extensions-lib 1.5
     */
    val title: String get() = throw Exception("Stub!")

    /**
     * See [com.sf.tadami.source.model.SAnime.rawTitle].
     *
     * @since extensions-lib 1.5
     */
    val rawTitle: String? get() = throw Exception("Stub!")

    /**
     * See [com.sf.tadami.source.model.SAnime.language].
     *
     * @since extensions-lib 1.6
     */
    val language: String? get() = throw Exception("Stub!")

    /**
     * The URL of its cover.
     *
     * @since extensions-lib 1.5
     */
    val thumbnailUrl: String? get() = throw Exception("Stub!")

    /**
     * When it was released, as its source gave it.
     *
     * @since extensions-lib 1.5
     */
    val release: String? get() = throw Exception("Stub!")

    /**
     * Its studio.
     *
     * @since extensions-lib 1.5
     */
    val studio: String? get() = throw Exception("Stub!")

    /**
     * Its author.
     *
     * @since extensions-lib 1.5
     */
    val author: String? get() = throw Exception("Stub!")

    /**
     * Its publication status.
     *
     * @since extensions-lib 1.5
     */
    val status: SAnimeStatus get() = throw Exception("Stub!")

    /**
     * Its synopsis.
     *
     * @since extensions-lib 1.5
     */
    val description: String? get() = throw Exception("Stub!")

    /**
     * Its genres.
     *
     * @since extensions-lib 1.5
     */
    val genres: List<String>? get() = throw Exception("Stub!")

    /**
     * Whether it's in the library.
     *
     * @since extensions-lib 1.5
     */
    val favorite: Boolean get() = throw Exception("Stub!")

    /**
     * When its episodes were last updated, in epoch milliseconds.
     *
     * @since extensions-lib 1.5
     */
    val lastUpdate: Long get() = throw Exception("Stub!")

    /**
     * When its episodes should be updated next, in epoch milliseconds.
     *
     * @since extensions-lib 1.5
     */
    val nextUpdate: Long get() = throw Exception("Stub!")

    /**
     * The days between two updates of its episodes.
     *
     * @since extensions-lib 1.5
     */
    val fetchInterval: Int get() = throw Exception("Stub!")

    /**
     * Whether its details were fetched.
     *
     * @since extensions-lib 1.5
     */
    val initialized: Boolean get() = throw Exception("Stub!")

    /**
     * How the app filters and shows its episodes.
     *
     * @since extensions-lib 1.5
     */
    val episodeFlags: Long get() = throw Exception("Stub!")

    /**
     * When it was added to the library, in epoch milliseconds.
     *
     * @since extensions-lib 1.5
     */
    val dateAdded: Long get() = throw Exception("Stub!")
}
