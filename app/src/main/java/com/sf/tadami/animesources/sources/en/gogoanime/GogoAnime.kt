package com.sf.tadami.animesources.sources.en.gogoanime

import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.animesources.extractors.DoodExtractor
import com.sf.tadami.animesources.extractors.Mp4uploadExtractor
import com.sf.tadami.animesources.extractors.StreamWishExtractor
import com.sf.tadami.animesources.sources.en.gogoanime.extractors.GogoCdnExtractor
import com.sf.tadami.animesources.sources.en.gogoanime.filters.GogoAnimeFilters
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.network.api.model.AnimeFilterList
import com.sf.tadami.network.api.model.SAnime
import com.sf.tadami.network.api.model.SEpisode
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.api.online.ConfigurableParsedHttpAnimeSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.okhttp.asCancelableObservable
import com.sf.tadami.network.requests.okhttp.asObservable
import com.sf.tadami.network.requests.utils.asJsoup
import com.sf.tadami.ui.tabs.settings.components.PreferenceScreen
import com.sf.tadami.ui.tabs.settings.model.CustomPreferences
import com.sf.tadami.ui.utils.parallelMap
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy

class GogoAnime() : ConfigurableParsedHttpAnimeSource<GogoAnimePreferences>() {

    override val id : String
        get() = "GogoAnime"

    override val name: String = "GogoAnime"
    override val baseUrl: String
        get() = preferences.baseUrl

    override val lang: Lang = Lang.ENGLISH

    private val ajaxBaseUrl: String = "https://ajax.gogo-load.com/ajax"

    override val client: OkHttpClient = network.cloudflareClient

    private val json: Json by injectLazy()
    override suspend fun getPrefGroup(): CustomPreferences<GogoAnimePreferences> {
        return GogoAnimePreferences
    }

    override fun getPreferenceScreen(navController: NavHostController): PreferenceScreen {
        return GogoAnimePreferencesScreen(navController,dataStore)
    }


    override fun getIconRes(): Int {
        return R.drawable.gogoanime
    }

    // Latest

    override fun latestSelector(): String {
        return "ul.items li"
    }

    override fun latestAnimeNextPageSelector(): String =
        "ul.pagination-list li:last-child:not(.selected)"

    override fun latestAnimeFromElement(element: Element): SAnime {
        val anime: SAnime = SAnime.create()
        val imgRef = element.select("div.img a").first()
        anime.title = element.select("p.name").first()!!.text()
        anime.url = getDetailsURL(imgRef?.select("img")?.first()?.attr("src"))
        anime.thumbnailUrl = imgRef!!.select("img").first()?.attr("src")
        return anime
    }

    private fun getDetailsURL(episodeURL: String?): String {
        return "/category/" + episodeURL?.split("/")?.last()
            ?.replace(Regex("(-[0-9]{5,}\\..*\$)|(\\..*\$)"), "")
    }

    override fun latestAnimesRequest(page: Int): Request {
        return GET("${ajaxBaseUrl}/page-recent-release.html?page=$page&type=1", headers)
    }

    // Search

    override fun searchSelector(): String = "div.img a"

    override fun searchAnimeNextPageSelector(): String =
        "ul.pagination-list li:last-child:not(.selected)"

    override fun searchAnimeFromElement(element: Element): SAnime {
        val anime: SAnime = SAnime.create()
        anime.title = element.attr("title")
        anime.thumbnailUrl = element.select("img").attr("src")
        anime.setUrlWithoutDomain(element.attr("href"))
        return anime
    }

    override fun searchAnimeRequest(
        page: Int,
        query: String,
        filters: AnimeFilterList,
        noToasts: Boolean
    ): Request {
        val params = GogoAnimeFilters.getSearchParameters(filters)

        val request = when {
            params.genre.isNotEmpty() -> GET("$baseUrl/genre/${params.genre}?page=$page", headers)
            params.recent.isNotEmpty() -> GET("https://ajax.gogo-load.com/ajax/page-recent-release.html?page=$page&type=${params.recent}", headers)
            params.season.isNotEmpty() -> GET("$baseUrl/${params.season}?page=$page", headers)
            else -> GET("$baseUrl/filter.html?keyword=$query&${params.filter}&page=$page", headers)
        }
        return request
    }

    // Details

    override fun animeDetailsParse(document: Document): SAnime {
        val anime = SAnime.create()

        anime.title = document.select("div.anime_info_body_bg h1").text()
        anime.genres = document.select("p.type:eq(5) a").map { it.attr("title") }
        anime.description = document.selectFirst("p.type:eq(4)")?.ownText()
        anime.status = document.select("p.type:eq(7) a").text()
        anime.release = document.selectFirst("p.type:eq(6)")?.ownText()

        return anime
    }

    // Episodes List

    override fun episodesSelector(): String = "li > a"

    override fun episodeFromElement(element: Element): SEpisode {
        val episode = SEpisode.create()
        val ep = element.selectFirst("div.name")?.ownText()?.substringAfter(" ") ?: ""
        episode.setUrlWithoutDomain(baseUrl + element.attr("href").substringAfter(" "))
        episode.name = "Episode $ep"
        episode.episodeNumber = ep.toFloat()
        return episode
    }

    private fun getGogoEpisodesRequest(response: Response): Request {
        val document = response.asJsoup()

        val lastEp = document.select("ul#episode_page li a").last()?.attr("ep_end")
        val animeId = document.select("input#movie_id").attr("value")

        return GET(
            "$ajaxBaseUrl/load-list-episode?ep_start=0&ep_end=$lastEp&id=$animeId",
            headers
        )
    }

    override fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> {
        val episodesListRequest = client.newCall(episodesRequest(anime))
            .asObservable()
            .map { response ->
                getGogoEpisodesRequest(response)
            }
        return episodesListRequest.flatMap { request ->
            client.newCall(request)
                .asCancelableObservable().map { response ->
                    episodesParse(response)
                }
        }
    }

    // Episode Source Stream

    override fun List<StreamSource>.sort(): List<StreamSource> {
        val quality = "1080"
        val server = "Gogostream"

        return this.sortedWith(
            compareBy(
                { it.quality.contains(quality) },
                { it.quality.contains(server) }
            )
        ).reversed()
    }

    override fun streamSourcesFromElement(element: Element): List<StreamSource> =
        throw Exception("not used")

    override fun streamSourcesSelector(): String = throw Exception("not used")

    override fun episodeSourcesParse(response: Response): List<StreamSource> {
        val document = response.asJsoup()
        val gogoExtractor = GogoCdnExtractor(network.client, json)
        val streamwishExtractor = StreamWishExtractor(client, headers)
        val streamSourcesList = mutableListOf<StreamSource>()
        streamSourcesList.addAll(
            document.select("div.anime_muti_link > ul > li").parallelMap { server ->
                runCatching {
                    val className = server.className()
                    val serverUrl = server.selectFirst("a")
                        ?.attr("data-video")
                        ?.replace(Regex("^//"), "https://")
                        ?: return@runCatching null
                    when (className) {
                        "anime", "vidcdn" -> {
                            gogoExtractor.videosFromUrl(serverUrl)
                        }
                        "streamwish" -> streamwishExtractor.videosFromUrl(serverUrl)
                        "doodstream" -> {
                            DoodExtractor(client).videosFromUrl(serverUrl)
                        }
                        "mp4upload" -> {
                            val headers = headers.newBuilder().set("Referer", "https://mp4upload.com/").build()
                            Mp4uploadExtractor(client).videosFromUrl(serverUrl, headers)
                        }
                        "filelions" -> {
                            streamwishExtractor.videosFromUrl(serverUrl, videoNameGen = { quality -> "FileLions - $quality" })
                        }
                        else -> null
                    }
                }.getOrNull()
            }.filterNotNull().flatten(),
        )
        return streamSourcesList.sort()
    }

    // Filters

    override fun getFilterList(): AnimeFilterList = GogoAnimeFilters.FILTER_LIST
}