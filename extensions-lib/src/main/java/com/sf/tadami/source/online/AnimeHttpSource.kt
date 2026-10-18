package com.sf.tadami.source.online

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.AnimesPage
import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.source.model.SAnime
import com.sf.tadami.source.model.SEpisode
import com.sf.tadami.source.model.StreamSource
import io.reactivex.rxjava3.core.Observable
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * A source of a website: each fetch runs the request its `...Request` method builds with [client],
 * then parses the response with its `...Parse` method. A source of lib 1.7 can override the suspend
 * methods instead, [getSearchAnime]... (it still implements the abstract methods, with a throwing
 * body then).
 *
 * @since extensions-lib 1.5
 */
abstract class AnimeHttpSource : AnimeCatalogueSource {

    /**
     * The app's HTTP clients.
     *
     * @since extensions-lib 1.5
     */
    protected val network: NetworkHelper = throw Exception("Stub!")

    /**
     * The URL of the website, without a trailing slash.
     *
     * @since extensions-lib 1.5
     */
    abstract val baseUrl: String

    /**
     * The headers of the requests, built once by [headersBuilder].
     *
     * @since extensions-lib 1.5
     */
    val headers: Headers = throw Exception("Stub!")

    /**
     * The client of the requests, [NetworkHelper.client] by default.
     *
     * @since extensions-lib 1.5
     */
    open val client: OkHttpClient
        get() = throw Exception("Stub!")

    /**
     * The headers of [headers], by default the app's User-Agent.
     *
     * @since extensions-lib 1.5
     */
    protected open fun headersBuilder() = Headers.Builder().apply {
        throw Exception("Stub")
    }

    // Search Animes

    /**
     * The request of the page [page] of the results of [query] with [filters].
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun searchAnimeRequest(page: Int, query: String, filters : AnimeFilterList, noToasts : Boolean): Request

    /**
     * The page of results in [response].
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun searchAnimeParse(response: Response): AnimesPage

    /**
     * [searchAnimeRequest], then [searchAnimeParse].
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Override getSearchAnime instead: the app calls the suspend methods, which default to the Rx ones")
    override fun fetchSearch(page: Int, query : String, filters: AnimeFilterList, noToasts : Boolean): Observable<AnimesPage> = throw Exception("Stub!")

    // Latest Animes

    /**
     * The request of the page [page] of the latest anime.
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun latestAnimesRequest(page: Int): Request

    /**
     * The page of latest anime in [response].
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun latestAnimeParse(response: Response): AnimesPage

    /**
     * [latestAnimesRequest], then [latestAnimeParse].
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Override getLatestAnime instead: the app calls the suspend methods, which default to the Rx ones")
    override fun fetchLatest(page: Int): Observable<AnimesPage> = throw Exception("Stub!")

    // Anime Details

    /**
     * The request of the details of [anime], by default a GET of [baseUrl] + its URL.
     *
     * @since extensions-lib 1.5
     */
    protected open fun animeDetailsRequest(anime: Anime): Request = throw Exception("Stub!")

    /**
     * The details in [response].
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun animeDetailsParse(response: Response): SAnime

    /**
     * [animeDetailsRequest], then [animeDetailsParse].
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Override getAnimeDetails instead: the app calls the suspend methods, which default to the Rx ones")
    override fun fetchAnimeDetails(anime: Anime): Observable<SAnime> = throw Exception("Stub!")

    // Episodes List

    /**
     * What [episodesListRequest] gives by default.
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Use episodesListRequest instead", ReplaceWith("episodesListRequest(anime)"))
    protected open fun episodesRequest(anime: Anime): Request = throw Exception("Stub!")

    /**
     * The request of the episodes of [anime], by default a GET of [baseUrl] + its URL.
     *
     * @since extensions-lib 1.5
     */
    protected open fun episodesListRequest(anime: Anime): Request = throw Exception("Stub!")

    /**
     * What [episodesListParse] gives by default.
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Use episodesListParse instead", ReplaceWith("episodesListParse(response)"))
    protected open fun episodesParse(response: Response): List<SEpisode> = throw Exception("Stub!")

    /**
     * The episodes in [response].
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun episodesListParse(response: Response): List<SEpisode>

    /**
     * [episodesListRequest], then [episodesListParse].
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Override getEpisodeList instead: the app calls the suspend methods, which default to the Rx ones")
    override fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> = throw Exception("Stub!")

   // Episode Sources

    /**
     * What [episodeSourcesRequest] gives by default.
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Use episodeSourcesRequest instead", ReplaceWith("episodeSourcesRequest(url)"))
    protected open fun episodeRequest(url: String): Request = throw Exception("Stub!")

    /**
     * The request of the streams of the episode of URL [url], by default a GET of [baseUrl] + [url].
     *
     * @since extensions-lib 1.5
     */
    protected open fun episodeSourcesRequest(url: String): Request = throw Exception("Stub!")

    /**
     * The streams in [response].
     *
     * @since extensions-lib 1.5
     */
    protected abstract fun episodeSourcesParse(response: Response): List<StreamSource>

    /**
     * [episodeSourcesRequest], then [episodeSourcesParse] and [sort].
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Override getEpisodeSources instead: the app calls the suspend methods, which default to the Rx ones")
    override fun fetchEpisode(url: String): Observable<List<StreamSource>> = throw Exception("Stub!")

    /**
     * [fetchEpisode].
     *
     * @since extensions-lib 1.5
     */
    @Deprecated("Override getEpisodeSources instead: the app calls the suspend methods, which default to the Rx ones")
    override fun fetchEpisodeSources(url: String): Observable<List<StreamSource>> = throw Exception("Stub!")

    /**
     * These streams in the order the player offers them, the first one being played. Unsorted by
     * default.
     *
     * @since extensions-lib 1.5
     */
    protected open fun List<StreamSource>.sort(): List<StreamSource> = throw Exception("Stub!")

    // Filters

    /**
     * No filter by default.
     *
     * @since extensions-lib 1.5
     */
    override fun getFilterList() : AnimeFilterList = throw Exception("Stub!")

    // Utils

    /**
     * Sets the URL of this anime to [url] without its scheme and domain.
     *
     * @since extensions-lib 1.5
     */
    fun SAnime.setUrlWithoutDomain(url: String) : Unit = throw Exception("Stub!")

    /**
     * Sets the URL of this episode to [url] without its scheme and domain.
     *
     * @since extensions-lib 1.5
     */
    fun SEpisode.setUrlWithoutDomain(url: String) : Unit = throw Exception("Stub!")

    private fun getUrlWithoutDomain(orig: String): String = throw Exception("Stub!")

    /**
     * Returns the url of the provided episode, the one the app opens in a browser.
     *
     * @since extensions-lib 1.1
     * @param episode the episode
     * @return url of the episode
     */
    open fun getEpisodeUrl(episode: SEpisode): String = throw Exception("Stub!")

}
