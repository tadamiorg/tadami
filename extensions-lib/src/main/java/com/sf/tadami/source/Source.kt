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

    @Deprecated("Use fetchEpisodeSources instead", ReplaceWith("fetchEpisodeSources(url)"))
    fun fetchEpisode(url: String): Observable<List<StreamSource>> = throw IllegalStateException("Not used")

    fun fetchEpisodeSources(url: String): Observable<List<StreamSource>> = throw IllegalStateException("Not used")

    fun getIconRes() : Int? = null

    /**
     * The details of [anime], what [fetchAnimeDetails] gives by default.
     *
     * @since extensions-lib 1.7
     */
    suspend fun getAnimeDetails(anime: SAnime): SAnime = throw Exception("Stub!")

    /**
     * The episodes of [anime], what [fetchEpisodesList] gives by default.
     *
     * @since extensions-lib 1.7
     */
    suspend fun getEpisodeList(anime: SAnime): List<SEpisode> = throw Exception("Stub!")

    /**
     * The streams of [episode], what [fetchEpisodeSources] gives by default.
     *
     * @since extensions-lib 1.7
     */
    suspend fun getEpisodeSources(episode: SEpisode): List<StreamSource> = throw Exception("Stub!")
}
