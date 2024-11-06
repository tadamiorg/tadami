package com.sf.tadami.source

import com.sf.tadami.App
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable

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
        return Observable.error(getSourceNotInstalledException())
    }

    private val isInvalid: Boolean = (name == "Unknown") || (lang == Lang.UNKNOWN) || (name.isBlank())

    override fun fetchLatest(page: Int): Observable<AnimesPage> {
        return Observable.error(getSourceNotInstalledException())
    }

    override fun getFilterList(): AnimeFilterList {
        throw getSourceNotInstalledException()
    }

    override fun fetchAnimeDetails(anime: Anime): Observable<SAnime> {
        return Observable.error(getSourceNotInstalledException())
    }

    override fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> {
        return Observable.error(getSourceNotInstalledException())
    }

    override fun fetchEpisode(url: String): Observable<List<StreamSource>> {
        return Observable.error(getSourceNotInstalledException())
    }

    private fun getSourceNotInstalledException(): SourceNotInstalledException {
        return SourceNotInstalledException()
    }

    override fun toString(): String {
        val appContext = App.getAppContext()
        if (appContext != null) {
            if (!isInvalid) "$name (${lang.getRes()})" else id.toString()
        }
        return if (!isInvalid) name else id.toString()
    }

    companion object {
        fun from(source: Source): StubSource {
            return StubSource(id = source.id, lang = source.lang, name = source.name)
        }
    }

    inner class SourceNotInstalledException : Exception("Id: $id - Name: $name")
}