package com.sf.tadami.network.api.online

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.network.api.model.SAnime
import com.sf.tadami.network.api.model.SEpisode
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable

interface Source {
    val id: String

    val name: String

    val lang: Lang

    fun fetchAnimeDetails(anime: Anime): Observable<SAnime> = throw IllegalStateException("Not used")

    fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> = throw IllegalStateException("Not used")

    fun fetchEpisode(url: String): Observable<List<StreamSource>> = throw IllegalStateException("Not used")

    fun getIconRes() : Int? = null

}