package com.sf.tadami.source.online

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.network.GET
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.asCancelableObservable
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
import uy.kohesive.injekt.injectLazy
import java.net.URI
import java.net.URISyntaxException

abstract class AnimeHttpSource : AnimeCatalogueSource {

    protected val network: NetworkHelper by injectLazy()

    abstract val baseUrl: String

    val headers: Headers by lazy { headersBuilder().build() }

    open val client: OkHttpClient
        get() = network.client

    protected open fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", network.advancedPreferences.userAgent)
    }

    // Search
    protected abstract fun searchAnimeRequest(page: Int, query: String, filters : AnimeFilterList, noToasts : Boolean): Request
    protected abstract fun searchAnimeParse(response: Response): AnimesPage

    override fun fetchSearch(page: Int, query : String, filters: AnimeFilterList, noToasts : Boolean): Observable<AnimesPage> {
        return client.newCall(searchAnimeRequest(page,query,filters,noToasts))
            .asCancelableObservable()
            .map { response ->
                searchAnimeParse(response)
            }

    }

    // Latest

    protected abstract fun latestAnimesRequest(page: Int): Request
    protected abstract fun latestAnimeParse(response: Response): AnimesPage

    override fun fetchLatest(page: Int): Observable<AnimesPage> {
        return client.newCall(latestAnimesRequest(page))
            .asCancelableObservable()
            .map { response ->
                latestAnimeParse(response)
            }
    }

    // Details

    protected open fun animeDetailsRequest(anime: Anime): Request {
        return GET(baseUrl+anime.url,headers)
    }
    protected abstract fun animeDetailsParse(response: Response): SAnime

    override fun fetchAnimeDetails(anime: Anime): Observable<SAnime> {
        return client.newCall(animeDetailsRequest(anime))
            .asCancelableObservable()
            .map { response ->
                animeDetailsParse(response)
            }
    }

    // Episodes

    protected open fun episodesRequest(anime: Anime): Request = GET(baseUrl+anime.url,headers)
    protected abstract fun episodesParse(response: Response): List<SEpisode>

    override fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> {
        return client.newCall(episodesRequest(anime))
            .asCancelableObservable()
            .map { response ->
                episodesParse(response)
            }
    }

   // Episode streams

    protected open fun episodeRequest(url: String): Request = GET(baseUrl+url,headers)
    protected abstract fun episodeSourcesParse(response: Response): List<StreamSource>

    override fun fetchEpisode(url: String): Observable<List<StreamSource>> {
        return client.newCall(episodeRequest(url))
            .asCancelableObservable()
            .map {
                episodeSourcesParse(it)
            }
    }

    protected open fun List<StreamSource>.sort(): List<StreamSource> {
        return this
    }

    // Filters

    override fun getFilterList() = AnimeFilterList()

    // Utils

    fun SAnime.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }
    fun SEpisode.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    private fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = URI(orig.replace(" ", "%20"))
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (e: URISyntaxException) {
            orig
        }
    }

}
