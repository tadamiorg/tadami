package com.sf.tadami.network.api.online

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.network.api.model.SAnime
import com.sf.tadami.network.api.model.SEpisode
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable

class StubSource(override val id: String) : Source {
    override val name: String = id
    override val lang: Lang = Lang.UNKNOWN

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

    inner class SourceNotInstalledException : Exception(id)
}