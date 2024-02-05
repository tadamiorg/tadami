package com.sf.tadami.source

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable

interface Source {
    val id: Long

    val name: String

    val lang: Lang

    fun fetchAnimeDetails(anime: Anime): Observable<SAnime> = throw IllegalStateException("Not used")

    fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> = throw IllegalStateException("Not used")

    fun fetchEpisode(url: String): Observable<List<StreamSource>> = throw IllegalStateException("Not used")

    fun getIconRes() : Int? = null

}