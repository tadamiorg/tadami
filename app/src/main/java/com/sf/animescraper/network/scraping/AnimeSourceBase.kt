package com.sf.animescraper.network.scraping

import com.sf.animescraper.network.requests.okhttp.HttpClient
import com.sf.animescraper.network.requests.okhttp.asObservable
import com.sf.animescraper.network.requests.utils.asJsoup
import com.sf.animescraper.network.scraping.dto.crypto.StreamSource
import com.sf.animescraper.network.scraping.dto.details.AnimeDetails
import com.sf.animescraper.network.scraping.dto.details.DetailsEpisode
import com.sf.animescraper.network.scraping.dto.search.Anime
import com.sf.animescraper.network.scraping.dto.search.AnimeFilterList
import io.reactivex.rxjava3.core.Observable
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.toImmutableList
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

    fun Anime.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    fun getUrlWithoutDomain(orig: String): String {
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

    // Latest functions

    protected abstract fun latestSelector(): String

    protected abstract fun latestAnimeFromElement(element: Element): Anime

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
            .asObservable()
            .map { response ->
                latestAnimeParse(response)
            }
    }

    // Search Functions

    protected abstract fun searchSelector(): String

    protected abstract fun searchAnimeFromElement(element: Element): Anime

    protected abstract fun searchAnimeRequest(page: Int,query: String,filters : AnimeFilterList): Request

    protected abstract fun searchAnimeNextPageSelector(): String?

    private fun searchAnimeParse(response: Response): AnimesPage {
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

    open fun fetchSearch(page: Int,query : String,filters: AnimeFilterList): Observable<AnimesPage> {
        return client.newCall(searchAnimeRequest(page,query,filters))
            .asObservable()
            .map { response ->
                searchAnimeParse(response)
            }

    }

    // Filters

    abstract fun getFilterList(): AnimeFilterList

    // Anime Details Functions

    protected abstract fun animeDetailsRequest(anime: Anime): Request

    private fun animeDetailsParse(response: Response): AnimeDetails {
        return animeDetailsParse(response.asJsoup())

    }

    protected abstract fun animeDetailsParse(document: Document): AnimeDetails

    open fun fetchAnimeDetails(anime: Anime): Observable<AnimeDetails> {
        return client.newCall(animeDetailsRequest(anime))
            .asObservable()
            .map { response ->
                animeDetailsParse(response)
            }
    }

    // Episodes List

    protected abstract fun episodesRequest(anime: Anime): Request

    protected abstract fun episodesSelector(): String

    protected abstract fun episodeFromElement(element: Element): DetailsEpisode

    protected open fun episodesParse(response: Response): List<DetailsEpisode> {
        val document = response.asJsoup()

        val episodes = document.select(episodesSelector()).map { element ->
            episodeFromElement(element)
        }
        return episodes
    }

    open fun fetchEpisodesList(anime: Anime): Observable<List<DetailsEpisode>> {
        return client.newCall(episodesRequest(anime))
            .asObservable()
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
            .asObservable()
            .map { response ->
                episodeSourcesParse(response)
            }
    }


}