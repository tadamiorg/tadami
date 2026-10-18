package com.sf.tadami.source

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable

@Deprecated("Stands for a missing source inside the app, not part of the extension API: it will be removed from extensions-lib")
class StubSource(
    override val id: Long,
    override val name: String = "Unknown",
    override val lang: Lang = Lang.UNKNOWN
) : AnimeCatalogueSource {
    override fun fetchSearch(
        page: Int,
        query: String,
        filters: AnimeFilterList,
        noToasts: Boolean
    ): Observable<AnimesPage> {
        throw Exception("Stub!")
    }

    override fun fetchLatest(page: Int): Observable<AnimesPage> {
        throw Exception("Stub!")
    }

    override fun getFilterList(): AnimeFilterList {
        throw Exception("Stub!")
    }

    override fun fetchAnimeDetails(anime: Anime): Observable<SAnime> {
        throw Exception("Stub!")
    }

    override fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> {
        throw Exception("Stub!")
    }

    override fun fetchEpisode(url: String): Observable<List<StreamSource>> {
        throw Exception("Stub!")
    }

    private fun getSourceNotInstalledException(): SourceNotInstalledException {
        throw Exception("Stub!")
    }

    companion object {
        fun from(source: Source): StubSource {
            throw Exception("Stub!")
        }
    }

    inner class SourceNotInstalledException : Exception("Id: $id - Name: $name")
}