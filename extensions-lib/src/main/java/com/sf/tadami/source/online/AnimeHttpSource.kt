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

abstract class AnimeHttpSource : AnimeCatalogueSource {

    protected val network: NetworkHelper = throw Exception("Stub!")

    abstract val baseUrl: String

    val headers: Headers = throw Exception("Stub!")

    open val client: OkHttpClient
        get() = throw Exception("Stub!")

    protected open fun headersBuilder() = Headers.Builder().apply {
        throw Exception("Stub")
    }

    // Search Animes
    protected abstract fun searchAnimeRequest(page: Int, query: String, filters : AnimeFilterList, noToasts : Boolean): Request
    protected abstract fun searchAnimeParse(response: Response): AnimesPage

    override fun fetchSearch(page: Int, query : String, filters: AnimeFilterList, noToasts : Boolean): Observable<AnimesPage> = throw Exception("Stub!")

    // Latest Animes

    protected abstract fun latestAnimesRequest(page: Int): Request
    protected abstract fun latestAnimeParse(response: Response): AnimesPage

    override fun fetchLatest(page: Int): Observable<AnimesPage> = throw Exception("Stub!")

    // Anime Details

    protected open fun animeDetailsRequest(anime: Anime): Request = throw Exception("Stub!")
    protected abstract fun animeDetailsParse(response: Response): SAnime

    override fun fetchAnimeDetails(anime: Anime): Observable<SAnime> = throw Exception("Stub!")

    // Episodes List

    @Deprecated("Use episodesListRequest instead", ReplaceWith("episodesListRequest(anime)"))
    protected open fun episodesRequest(anime: Anime): Request = throw Exception("Stub!")
    protected open fun episodesListRequest(anime: Anime): Request = throw Exception("Stub!")

    @Deprecated("Use episodesListParse instead", ReplaceWith("episodesListParse(response)"))
    protected open fun episodesParse(response: Response): List<SEpisode> = throw Exception("Stub!")
    protected abstract fun episodesListParse(response: Response): List<SEpisode>

    override fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> = throw Exception("Stub!")

   // Episode Sources

    @Deprecated("Use episodeSourcesRequest instead", ReplaceWith("episodeSourcesRequest(url)"))
    protected open fun episodeRequest(url: String): Request = throw Exception("Stub!")
    protected open fun episodeSourcesRequest(url: String): Request = throw Exception("Stub!")

    protected abstract fun episodeSourcesParse(response: Response): List<StreamSource>

    @Deprecated("Use fetchEpisodeSources instead", ReplaceWith("fetchEpisodeSources(url)"))
    override fun fetchEpisode(url: String): Observable<List<StreamSource>> = throw Exception("Stub!")
    override fun fetchEpisodeSources(url: String): Observable<List<StreamSource>> = throw Exception("Stub!")

    protected open fun List<StreamSource>.sort(): List<StreamSource> = throw Exception("Stub!")

    // Filters

    override fun getFilterList() : AnimeFilterList = throw Exception("Stub!")

    // Utils

    fun SAnime.setUrlWithoutDomain(url: String) : Unit = throw Exception("Stub!")
    fun SEpisode.setUrlWithoutDomain(url: String) : Unit = throw Exception("Stub!")

    private fun getUrlWithoutDomain(orig: String): String = throw Exception("Stub!")

    /**
     * Returns the url of the provided episode
     *
     * @since extensions-lib 1.1
     * @param episode the episode
     * @return url of the episode
     */
    open fun getEpisodeUrl(episode: SEpisode): String = throw Exception("Stub!")

}
