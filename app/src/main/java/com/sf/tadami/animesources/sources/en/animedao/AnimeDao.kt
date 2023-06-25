package com.sf.tadami.animesources.sources.en.animedao

import com.sf.tadami.R
import com.sf.tadami.animesources.extractors.DoodExtractor
import com.sf.tadami.animesources.extractors.Mp4uploadExtractor
import com.sf.tadami.animesources.extractors.streamsbextractor.StreamSBExtractor
import com.sf.tadami.animesources.sources.en.animedao.extractors.MixDropExtractor
import com.sf.tadami.animesources.sources.en.animedao.extractors.VidstreamingExtractor
import com.sf.tadami.network.api.model.AnimeFilterList
import com.sf.tadami.network.api.model.SAnime
import com.sf.tadami.network.api.model.SEpisode
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.api.online.AnimeSource
import com.sf.tadami.network.api.online.AnimesPage
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.okhttp.asObservableSuccess
import com.sf.tadami.network.requests.utils.asJsoup
import com.sf.tadami.ui.utils.capFirstLetter
import com.sf.tadami.ui.utils.parallelMap
import com.sf.tadami.utils.Lang
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.*

class AnimeDao : AnimeSource("AnimeDao") {
    override val name = "AnimeDao"

    override val baseUrl = "https://animedao.to"

    override val lang = Lang.ENGLISH

    override val client: OkHttpClient = network.cloudflareClient

    override fun getIconRes(): Int {
        return R.drawable.animedao
    }

    private val json: Json by injectLazy()

    // =============================== Latest ===============================

    override fun latestAnimesRequest(page: Int): Request = GET(baseUrl)
    override fun latestAnimeNextPageSelector(): String? = null

    override fun latestSelector(): String = "div#latest-tab-pane > div.row > div.col-md-6"

    override fun latestAnimeFromElement(element: Element): SAnime {
        val thumbnailUrl = element.selectFirst("img")!!.attr("data-src")

        return SAnime.create().apply {
            setUrlWithoutDomain(element.selectFirst("a.animeparent")!!.attr("href"))
            this.thumbnailUrl = if (thumbnailUrl.contains(baseUrl.toHttpUrl().host)) {
                thumbnailUrl
            } else {
                baseUrl + thumbnailUrl
            }
            title = element.selectFirst("span.animename")!!.text()
        }
    }

    // =============================== Search ===============================

    override fun searchAnimeRequest(
        page: Int,
        query: String,
        filters: AnimeFilterList,
        noToasts: Boolean
    ): Request = throw Exception("Not used")

    override fun fetchSearch(
        page: Int,
        query: String,
        filters: AnimeFilterList,
        noToasts: Boolean
    ): io.reactivex.rxjava3.core.Observable<AnimesPage> {
        val params = AnimeDaoFilters.getSearchParameters(filters)
        return client.newCall(customSearchAnimeRequest(page, query, params))
            .asObservableSuccess()
            .map { response ->
                searchAnimeParse(response)
            }
    }

    private fun customSearchAnimeRequest(
        page: Int,
        query: String,
        filters: AnimeDaoFilters.FilterSearchParams
    ): Request {

        return when {
            query.isNotBlank() -> {
                val cleanQuery = query.replace(" ", "+")
                GET("$baseUrl/search/?search=$cleanQuery", headers = headers)
            }
            filters.isBlank() -> {
                GET("$baseUrl/animelist/popular", headers = headers)
            }
            else -> {
                var url = "$baseUrl/animelist/".toHttpUrlOrNull()!!.newBuilder()
                    .addQueryParameter("status[]=", filters.status)
                    .addQueryParameter("order[]=", filters.order)
                    .build().toString()
                if (filters.genre.isNotBlank()) url += "&${filters.genre}"
                if (filters.rating.isNotBlank()) url += "&${filters.rating}"
                if (filters.letter.isNotBlank()) url += "&${filters.letter}"
                if (filters.year.isNotBlank()) url += "&${filters.year}"
                if (filters.score.isNotBlank()) url += "&${filters.score}"
                url += "&page=$page"
                GET(url, headers = headers)
            }
        }
    }


    override fun searchAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()
        val selector = if (response.request.url.encodedPath.startsWith("/animelist/") && !response.request.url.encodedPath.contains("popular")) {
                searchAnimeSelectorFilter()
            } else {
                searchSelector()
            }

        val animes = document.select(selector).map { element ->
            searchAnimeFromElement(element)
        }

        val hasNextPage = searchAnimeNextPageSelector().let { select ->
            document.select(select).first()
        } != null

        return AnimesPage(animes, hasNextPage)
    }


    override fun searchSelector(): String = "div.container > div.row > div.col-md-6"

    override fun searchAnimeFromElement(element: Element): SAnime {
        val thumbnailUrl = element.selectFirst("img")!!.attr("data-src")

        return SAnime.create().apply {
            setUrlWithoutDomain(element.selectFirst("a")!!.attr("href"))
            this.thumbnailUrl = if (thumbnailUrl.contains(baseUrl.toHttpUrl().host)) {
                thumbnailUrl
            } else {
                baseUrl + thumbnailUrl
            }
            title = element.selectFirst("span.animename")!!.text()
        }
    }

    private fun searchAnimeSelectorFilter(): String =
        "div.container div.col-12 > div.row > div.col-md-6"

    override fun searchAnimeNextPageSelector(): String =
        "ul.pagination > li.page-item:has(i.fa-arrow-right):not(.disabled)"

    // ============================== FILTERS ===============================

    override fun getFilterList(): AnimeFilterList = AnimeDaoFilters.FILTER_LIST

    // =========================== Anime Details ============================

    override fun animeDetailsParse(document: Document): SAnime {
        val thumbnailUrl = document.selectFirst("div.card-body img")!!.attr("data-src")
        val moreInfo =
            document.select("div.card-body table > tbody > tr").joinToString("\n") { it.text() }

        return SAnime.create().apply {
            title = document.selectFirst("div.card-body h2")!!.text()
            this.thumbnailUrl = if (thumbnailUrl.contains(baseUrl.toHttpUrl().host)) {
                thumbnailUrl
            } else {
                baseUrl + thumbnailUrl
            }
            status =
                document.selectFirst("div.card-body table > tbody > tr:has(>td:contains(Status)) td:not(:contains(Status))")
                    ?.text()
            description =
                (document.selectFirst("div.card-body div:has(>b:contains(Description))")?.ownText()
                    ?: "") + "\n\n$moreInfo"
            genres =
                document.select("div.card-body table > tbody > tr:has(>td:contains(Genres)) td > a")
                    .map { it.text() }
        }
    }

    // ============================== Episodes ==============================


    override fun episodesParse(response: Response): List<SEpisode> {
        return super.episodesParse(response).sortedWith(
            compareBy(
                { it.episodeNumber },
                { it.name },
            ),
        ).reversed()
    }

    override fun streamSourcesSelector(): String = throw Exception("Not used")

    override fun streamSourcesFromElement(element: Element): List<StreamSource> =
        throw Exception("Not used")

    override fun episodesSelector(): String = "div#episodes-tab-pane > div.row > div > div.card"

    override fun episodeFromElement(element: Element): SEpisode {
        val episodeName = element.selectFirst("span.animename")!!.text()
        val episodeTitle = element.selectFirst("div.animetitle")?.text() ?: ""

        return SEpisode.create().apply {
            name = "$episodeName $episodeTitle"
            episodeNumber = if (episodeName.contains("Episode ", true)) {
                episodeName.substringAfter("Episode ").substringBefore(" ").toFloatOrNull() ?: 0F
            } else {
                0F
            }
            dateUpload = element.selectFirst("span.date")?.let { parseDate(it.text()) } ?: 0L
            setUrlWithoutDomain(element.selectFirst("a[href]")!!.attr("href"))
        }
    }

    // ============================ Video Links =============================

    @OptIn(ExperimentalSerializationApi::class)
    override fun episodeSourcesParse(response: Response): List<StreamSource> {
        val document = response.asJsoup()
        val videoList = mutableListOf<StreamSource>()
        val serverList = mutableListOf<Server>()
        val script = document.selectFirst("script:containsData(videowrapper)")!!.data()
        val frameRegex = """function (\w+).*?iframe src="(.*?)"""".toRegex()

        frameRegex.findAll(script).forEach {
            val redirected =
                client.newCall(GET(baseUrl + it.groupValues[2])).execute().request.url.toString()
            serverList.add(
                Server(
                    redirected,
                    it.groupValues[1],
                ),
            )
        }

        // Get videos
        videoList.addAll(
            serverList.parallelMap { server ->
                runCatching {
                    val prefix = server.name.capFirstLetter()
                    val url = server.url

                    when {
                        url.contains("streamsb") -> {
                            StreamSBExtractor(client).videosFromUrl(
                                url,
                                headers = headers,
                                prefix = prefix
                            )
                        }
                        url.contains("vidstreaming") -> {
                            VidstreamingExtractor(client, json).videosFromUrl(url, prefix = prefix)
                        }
                        url.contains("mixdrop") -> {
                            MixDropExtractor(client).videoFromUrl(url)
                        }
                        url.contains("https://dood") -> {
                            DoodExtractor(client).videosFromUrl(url, quality = server.name)
                        }
                        url.contains("mp4upload") -> {
                            Mp4uploadExtractor(client).videosFromUrl(url, headers, prefix)
                        }
                        else -> null
                    }
                }.getOrNull()
            }.filterNotNull().flatten(),
        )

        return videoList.sort()
    }

    // ============================= Utilities ==============================

    private fun List<StreamSource>.sort(): List<StreamSource> {
        val quality = PREF_QUALITY_DEFAULT
        val server = PREF_SERVER_DEFAULT

        return this.sortedWith(
            compareBy(
                { it.quality.contains(quality) },
                { it.quality.contains(server) },
            ),
        ).reversed()
    }

    data class Server(
        val url: String,
        val name: String,
    )

    private fun parseDate(dateStr: String): Long {
        return runCatching { DATE_FORMATTER.parse(dateStr)?.time }
            .getOrNull() ?: 0L
    }

    companion object {
        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)
        }
        private const val PREF_QUALITY_DEFAULT = "1080"
        private const val PREF_SERVER_DEFAULT = "vstream"
    }
}