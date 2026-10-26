package com.sf.tadami.domain.anime

import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SAnimeStatus

/**
 * [anime] seen as the [SAnime] the suspend source methods take, see [toSAnime]. Their defaults give
 * the Rx fetches of the extensions built before lib 1.7 the whole [Anime] back, as these got it
 * before, see [asLegacyAnime]. A field set through it updates [anime].
 */
class DomainBackedSAnime(anime: Anime) : SAnime {

    @Volatile
    var anime: Anime = anime
        private set

    override var url: String
        get() = anime.url
        set(value) { anime = anime.copy(url = value) }
    override var title: String
        get() = anime.title
        set(value) { anime = anime.copy(title = value) }
    override var rawTitle: String?
        get() = anime.rawTitle
        set(value) { anime = anime.copy(rawTitle = value) }
    override var language: String?
        get() = anime.language
        set(value) { anime = anime.copy(language = value) }
    override var thumbnailUrl: String?
        get() = anime.thumbnailUrl
        set(value) { anime = anime.copy(thumbnailUrl = value) }
    override var release: String?
        get() = anime.release
        set(value) { anime = anime.copy(release = value) }
    override var studio: String?
        get() = anime.studio
        set(value) { anime = anime.copy(studio = value) }
    override var author: String?
        get() = anime.author
        set(value) { anime = anime.copy(author = value) }
    override var status: SAnimeStatus
        get() = anime.status
        set(value) { anime = anime.copy(status = value) }
    override var description: String?
        get() = anime.description
        set(value) { anime = anime.copy(description = value) }
    override var genres: List<String>?
        get() = anime.genres
        set(value) { anime = anime.copy(genres = value) }
    override var initialized: Boolean
        get() = anime.initialized
        set(value) { anime = anime.copy(initialized = value) }

    override fun toString(): String = "DomainBackedSAnime($anime)"
}

/** This anime as the [SAnime] the suspend source methods take, see [DomainBackedSAnime]. */
fun Anime.toSAnime(): SAnime = DomainBackedSAnime(this)

/**
 * The [Anime] the Rx fetches of a source take: the one [DomainBackedSAnime] wraps, otherwise this
 * anime of the source [sourceId], see [toDomainAnime].
 */
fun SAnime.asLegacyAnime(sourceId: Long): Anime = (this as? DomainBackedSAnime)?.anime ?: toDomainAnime(sourceId)
