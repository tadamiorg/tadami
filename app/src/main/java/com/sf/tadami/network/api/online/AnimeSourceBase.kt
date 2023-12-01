package com.sf.tadami.network.api.online

import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.network.api.model.AnimeFilterList
import com.sf.tadami.network.api.model.SAnime
import com.sf.tadami.network.api.model.SEpisode
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.requests.okhttp.HttpClient
import com.sf.tadami.network.requests.okhttp.asCancelableObservable
import com.sf.tadami.network.requests.utils.asJsoup
import io.reactivex.rxjava3.core.Observable
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.net.URI
import java.net.URISyntaxException

abstract class AnimeSourceBase {

    // HttpClient related work

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36 Edg/88.0.705.63"
    }

    protected val network: HttpClient by injectLazy()

    protected open val client = network.client

    protected val headers: Headers by lazy { headersBuilder().build() }

    protected open fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", DEFAULT_USER_AGENT)
    }

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

    abstract fun getIconRes() : Int?

    // Latest functions

    protected abstract fun latestSelector(): String

    protected abstract fun latestAnimeFromElement(element: Element): SAnime

    protected abstract fun latestAnimesRequest(page: Int): Request

    protected abstract fun latestAnimeNextPageSelector(): String?

    private fun latestAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        val animes = document.select(latestSelector()).map { element ->
            latestAnimeFromElement(element)
        }

        val hasNextPage = latestAnimeNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return AnimesPage(animes, hasNextPage)
    }

    open fun fetchLatest(page: Int): Observable<AnimesPage> {
        return client.newCall(latestAnimesRequest(page))
            .asCancelableObservable()
            .map { response ->
                latestAnimeParse(response)
            }
    }

    // Search Functions

    protected abstract fun searchSelector(): String

    protected abstract fun searchAnimeFromElement(element: Element): SAnime

    protected abstract fun searchAnimeRequest(page: Int,query: String,filters : AnimeFilterList,noToasts : Boolean): Request

    protected abstract fun searchAnimeNextPageSelector(): String?

    protected open fun searchAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        val animes = document.select(searchSelector()).map { element ->
            val test = searchAnimeFromElement(element)
            test
        }

        val hasNextPage = searchAnimeNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return AnimesPage(animes, hasNextPage)
    }

    open fun fetchSearch(page: Int,query : String,filters: AnimeFilterList,noToasts : Boolean = false): Observable<AnimesPage> {
        return client.newCall(searchAnimeRequest(page,query,filters,noToasts))
            .asCancelableObservable()
            .map { response ->
                searchAnimeParse(response)
            }

    }

    // Filters

    abstract fun getFilterList(): AnimeFilterList

    // Anime Details Functions

    protected abstract fun animeDetailsRequest(anime: Anime): Request

    protected fun animeDetailsParse(response: Response): SAnime {
        return animeDetailsParse(response.asJsoup())
    }

    protected abstract fun animeDetailsParse(document: Document): SAnime

    open fun fetchAnimeDetails(anime: Anime): Observable<SAnime> {
        return client.newCall(animeDetailsRequest(anime))
            .asCancelableObservable()
            .map { response ->
                animeDetailsParse(response)
            }
    }

    // Episodes List

    protected abstract fun episodesRequest(anime: Anime): Request

    protected abstract fun episodesSelector(): String

    protected abstract fun episodeFromElement(element: Element): SEpisode

    protected open fun episodesParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()

        val episodes = document.select(episodesSelector()).map { element ->
            episodeFromElement(element)
        }
        return episodes
    }

    open fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> {
        return client.newCall(episodesRequest(anime))
            .asCancelableObservable()
            .map { response ->
                episodesParse(response)
            }
    }

    // Get Episode Sources and Link

    protected abstract fun episodeRequest(url: String): Request

    protected abstract fun streamSourcesSelector(): String

    protected abstract fun streamSourcesFromElement(element: Element): List<StreamSource>

    protected open fun episodeSourcesParse(response: Response): List<StreamSource> {
        val document = response.asJsoup()

        val sources = document.select(streamSourcesSelector()).flatMap { source ->
            streamSourcesFromElement(source)
        }
        return sources
    }

    open fun fetchEpisode(url: String): Observable<List<StreamSource>> {
        return client.newCall(episodeRequest(url))
            .asCancelableObservable()
            .map {
                episodeSourcesParse(it)
            }
    }


}