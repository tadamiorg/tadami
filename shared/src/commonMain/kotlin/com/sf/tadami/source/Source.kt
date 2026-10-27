package com.sf.tadami.source

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.asLegacyAnime
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.rx3.awaitSingle

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

    // What the app calls. Each one defaults to its Rx fetch, which the extensions built before lib
    // 1.7 implement. AnimeHttpSource must not override them: the extensions overriding the Rx fetches
    // would be bypassed.

    /**
     * The details of [anime], what [fetchAnimeDetails] gives by default.
     *
     * @since extensions-lib 1.7
     */
    suspend fun getAnimeDetails(anime: SAnime): SAnime =
        fetchAnimeDetails(anime.asLegacyAnime(id)).awaitSingle()

    /**
     * The episodes of [anime], what [fetchEpisodesList] gives by default.
     *
     * @since extensions-lib 1.7
     */
    suspend fun getEpisodeList(anime: SAnime): List<SEpisode> =
        fetchEpisodesList(anime.asLegacyAnime(id)).awaitSingle()

    /**
     * The streams of [episode], what [fetchEpisodeSources] gives by default.
     *
     * @since extensions-lib 1.7
     */
    suspend fun getEpisodeSources(episode: SEpisode): List<StreamSource> =
        fetchEpisodeSources(episode.url).awaitSingle()
}
