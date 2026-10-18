package com.sf.tadami.source

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable

/**
 * Stands in the app for a source whose extension isn't installed: every call fails with
 * [SourceNotInstalledException].
 *
 * @since extensions-lib 1.5
 */
@Deprecated("Stands for a missing source inside the app, not part of the extension API: it will be removed from extensions-lib")
class StubSource(
    override val id: Long,
    override val name: String = "Unknown",
    override val lang: Lang = Lang.UNKNOWN
) : AnimeCatalogueSource {
    @Deprecated("Override getSearchAnime instead: the app calls the suspend methods, which default to the Rx ones")
    override fun fetchSearch(
        page: Int,
        query: String,
        filters: AnimeFilterList,
        noToasts: Boolean
    ): Observable<AnimesPage> {
        throw Exception("Stub!")
    }

    @Deprecated("Override getLatestAnime instead: the app calls the suspend methods, which default to the Rx ones")
    override fun fetchLatest(page: Int): Observable<AnimesPage> {
        throw Exception("Stub!")
    }

    override fun getFilterList(): AnimeFilterList {
        throw Exception("Stub!")
    }

    @Deprecated("Override getAnimeDetails instead: the app calls the suspend methods, which default to the Rx ones")
    override fun fetchAnimeDetails(anime: Anime): Observable<SAnime> {
        throw Exception("Stub!")
    }

    @Deprecated("Override getEpisodeList instead: the app calls the suspend methods, which default to the Rx ones")
    override fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> {
        throw Exception("Stub!")
    }

    @Deprecated("Override getEpisodeSources instead: the app calls the suspend methods, which default to the Rx ones")
    override fun fetchEpisode(url: String): Observable<List<StreamSource>> {
        throw Exception("Stub!")
    }

    private fun getSourceNotInstalledException(): SourceNotInstalledException {
        throw Exception("Stub!")
    }

    companion object {
        /**
         * A stub of [source].
         *
         * @since extensions-lib 1.5
         */
        fun from(source: Source): StubSource {
            throw Exception("Stub!")
        }
    }

    /**
     * What the calls to a missing source fail with.
     *
     * @since extensions-lib 1.5
     */
    inner class SourceNotInstalledException : Exception("Id: $id - Name: $name")
}
