package com.sf.tadami.source

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable

/**
 * A source of anime. The app calls its suspend methods ([getAnimeDetails]...), from a background
 * thread. Each one defaults to its Rx fetch ([fetchAnimeDetails]...), the API before lib 1.7:
 * override either, the suspend one in a new extension.
 *
 * @since extensions-lib 1.5
 */
interface Source {
    /**
     * Its ID, which the app stores with its anime, its preferences and its downloads: never
     * change it. Unique: the app refuses an extension whose source has the ID of another one.
     *
     * @since extensions-lib 1.5
     */
    val id: Long

    /**
     * Its name, shown in the app.
     *
     * @since extensions-lib 1.5
     */
    val name: String

    /**
     * The language of its anime.
     *
     * @since extensions-lib 1.5
     */
    val lang: Lang

    /**
     * The details of [anime], what [getAnimeDetails] gives by default.
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Override getAnimeDetails instead: the app calls the suspend methods, which default to the Rx ones")
    fun fetchAnimeDetails(anime: Anime): Observable<SAnime> = throw IllegalStateException("Not used")

    /**
     * The episodes of [anime], what [getEpisodeList] gives by default.
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Override getEpisodeList instead: the app calls the suspend methods, which default to the Rx ones")
    fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> = throw IllegalStateException("Not used")

    /**
     * What [fetchEpisodeSources] gives by default.
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Override getEpisodeSources instead: the app calls the suspend methods, which default to the Rx ones")
    fun fetchEpisode(url: String): Observable<List<StreamSource>> = throw IllegalStateException("Not used")

    /**
     * The streams of the episode of URL [url], what [getEpisodeSources] gives by default.
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Override getEpisodeSources instead: the app calls the suspend methods, which default to the Rx ones")
    fun fetchEpisodeSources(url: String): Observable<List<StreamSource>> = throw IllegalStateException("Not used")

    /**
     * Unused: the app shows the icon of the extension.
     *
     * @since extensions-lib 1.5
     */
    fun getIconRes() : Int? = null

    /**
     * The details of [anime], what [fetchAnimeDetails] gives by default.
     *
     * @param anime an anime of this source, with at least its [SAnime.url] and [SAnime.title]
     * @since extensions-lib 1.7
     */
    suspend fun getAnimeDetails(anime: SAnime): SAnime = throw Exception("Stub!")

    /**
     * The episodes of [anime], what [fetchEpisodesList] gives by default.
     *
     * @param anime an anime of this source, with at least its [SAnime.url] and [SAnime.title]
     * @since extensions-lib 1.7
     */
    suspend fun getEpisodeList(anime: SAnime): List<SEpisode> = throw Exception("Stub!")

    /**
     * The streams of [episode], what [fetchEpisodeSources] gives by default.
     *
     * @param episode an episode of this source, with at least its [SEpisode.url]
     * @since extensions-lib 1.7
     */
    suspend fun getEpisodeSources(episode: SEpisode): List<StreamSource> = throw Exception("Stub!")
}
