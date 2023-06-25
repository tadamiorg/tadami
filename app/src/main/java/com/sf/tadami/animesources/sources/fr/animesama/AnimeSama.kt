package com.sf.tadami.animesources.sources.fr.animesama

import android.text.Html
import com.sf.tadami.R
import com.sf.tadami.animesources.sources.fr.animesama.extractors.MyViExtractor
import com.sf.tadami.animesources.sources.fr.animesama.extractors.SendVidExtractor
import com.sf.tadami.animesources.sources.fr.animesama.extractors.SibNetExtractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.network.api.model.AnimeFilterList
import com.sf.tadami.network.api.model.SAnime
import com.sf.tadami.network.api.model.SEpisode
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.api.online.AnimeSource
import com.sf.tadami.network.api.online.AnimesPage
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.okhttp.POST
import com.sf.tadami.network.requests.okhttp.asCancelableObservable
import com.sf.tadami.network.requests.okhttp.asObservable
import com.sf.tadami.network.requests.utils.asJsoup
import com.sf.tadami.ui.utils.parallelMap
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class AnimeSama : AnimeSource("AnimeSama") {

    override val name: String = "AnimeSama"

    override val baseUrl: String = "https://anime-sama.fr"

    override val lang: Lang = Lang.FRENCH

    override val client: OkHttpClient = network.cloudflareClient

    override fun getIconRes(): Int {
        return R.drawable.animesama
    }

    private var episodeNumber: Int? = null

    override val supportRecent = false

    override fun latestSelector(): String = throw Exception("Not used")

    override fun latestAnimeNextPageSelector(): String = throw Exception("Not used")

    override fun latestAnimeFromElement(element: Element): SAnime = throw Exception("Not used")

    override fun latestAnimesRequest(page: Int): Request = throw Exception("Not used")

    override fun searchSelector(): String =
        "div.cardListAnime.Anime, div.cardListAnime[class*=\"Anime,\"], div.cardListAnime[class*=\",Anime\"]"

    override fun fetchSearch(
        page: Int,
        query: String,
        filters: AnimeFilterList,
        noToasts: Boolean
    ): Observable<AnimesPage> {
        return client.newCall(searchAnimeRequest(page, query, filters, noToasts))
            .asObservable()
            .flatMap { response ->

                val document = response.asJsoup()

                val animeList = document.select(searchSelector()).map { element ->
                    searchAnimeFromElement(element)
                }

                val hasNextPage = searchAnimeNextPageSelector().let { selector ->
                    document.select(selector).first()
                } != null

                val pageRequests = animeList.map { anime ->
                    client.newCall(GET("$baseUrl${anime.url}"))
                        .asObservable()
                        .map { response ->
                            val doc = response.asJsoup()
                            val seasonDiv =
                                doc.selectFirst("h2:contains(Anime) ~ div:has(script)")
                            val seasonScript =
                                seasonDiv?.selectFirst("script")?.data()?.trimIndent()
                            val seasonsMatches = seasonScript?.let {
                                Regex("""[^(/*)]panneauAnime\("(.*?)",\s*"(.*?)"\)""").findAll(it)
                            }
                            val seasons = seasonsMatches?.map { matchResult ->
                                val (param1, param2) = matchResult.destructured
                                param1 to param2
                            }?.toList()

                            seasons?.map { (seasonName, seasonUrl) ->
                                val animeSeason = SAnime.create()
                                animeSeason.url = "${anime.url}/$seasonUrl"
                                animeSeason.thumbnailUrl = anime.thumbnailUrl
                                animeSeason.title = "$seasonName - ${anime.title}"
                                animeSeason
                            } ?: emptyList<SAnime>()
                        }
                }

                Observable.zip(pageRequests) { pages ->
                    val animeSeasons = pages.flatMap { it as List<SAnime> }
                    AnimesPage(animeSeasons, hasNextPage)
                }

            }
    }

    override fun searchAnimeRequest(
        page: Int,
        query: String,
        filters: AnimeFilterList,
        noToasts: Boolean
    ): Request {

        return when {
            query.isNotEmpty() -> {
                val formData = FormBody.Builder()
                    .add("query", query)
                    .build()
                POST("$baseUrl/catalogue/searchbar.php", headers, formData)
            }
            else -> {
                GET("$baseUrl/catalogue/index.php?page=$page", headers)
            }
        }
    }

    override fun searchAnimeFromElement(element: Element): SAnime {
        val anime: SAnime = SAnime.create()
        anime.title = element.selectFirst("h1")?.text() ?: ""
        anime.thumbnailUrl = element.select("img").attr("src")
        anime.setUrlWithoutDomain(element.selectFirst("a")!!.attr("href"))
        return anime
    }


    override fun searchAnimeNextPageSelector(): String = "div#nav_pages a.bg-sky-900 ~ a"

    override fun animeDetailsRequest(anime: Anime): Request {
        return GET(baseUrl + anime.url + "/../..", headers)
    }

    override fun animeDetailsParse(document: Document): SAnime {
        val anime = SAnime.create()
        anime.title = document.selectFirst("#titreOeuvre")!!.text()
        anime.description = document.selectFirst("meta[name=description]")?.attr("content")
        anime.genres = document.selectFirst("h2:contains(Genres) ~ a")?.text()?.trim()?.split(",")
        anime.thumbnailUrl = document.selectFirst("meta[itemprop=image]")?.attr("content")
        return anime
    }

    override fun episodesSelector(): String = throw Exception("Not used")

    override fun episodeFromElement(element: Element): SEpisode = throw Exception("Not used")

    override fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> {
        return client.newCall(episodesRequest(anime))
            .asObservable()
            .flatMap { response ->
                val document = response.asJsoup()
                val episodeScriptFile =
                    document.selectFirst("script[src^=episodes.js?filever]")?.attr("src")
                val episodeScriptRequest = GET(baseUrl + anime.url + "/$episodeScriptFile", headers)
                client.newCall(episodeScriptRequest)
                    .asObservable()
                    .map { res ->
                        val doc = res.asJsoup()
                        val pattern = Regex("""var (.*?) = \[(.*?)\];""")
                        val matches = pattern.findAll(doc.html())

                        val variableLinkCounts: MutableMap<String, Int> = mutableMapOf()

                        for (matchResult in matches) {
                            val variableName = matchResult.groupValues[1]
                            val urls = matchResult.groupValues[2]
                                .split(",")
                                .map { it.trim('\'', '"', ' ', '\n', '\r') }

                            val linkCount = urls.size
                            variableLinkCounts[variableName] = linkCount
                        }
                        val episodesList: MutableList<SEpisode> = mutableListOf()
                        val variableWithMostLinks = variableLinkCounts.maxByOrNull { it.value }

                        for (i in 1..(variableWithMostLinks?.value?.minus(1) ?: 0)) {
                            val episode = SEpisode.create()
                            episode.episodeNumber = i.toFloat()
                            episode.name = "Episode ${i.toFloat()}"
                            episode.url = "${anime.url}?number=$i"
                            episodesList.add(episode)
                        }
                        episodesList.reversed()
                    }
            }
    }

    override fun streamSourcesSelector(): String = throw Exception("Not used")

    override fun streamSourcesFromElement(element: Element): List<StreamSource> =
        throw Exception("Not used")

    private fun List<StreamSource>.sort(): List<StreamSource> {
        val server = "AnimeSama"

        return this.sortedWith(
            compareBy { it.quality.contains(server) }
        ).reversed()
    }

    override fun episodeSourcesParse(response: Response): List<StreamSource> {

        val document: Document = response.asJsoup()
        val javascriptCode = Html.fromHtml(document.html(), Html.FROM_HTML_MODE_LEGACY).toString()
        val pattern = Regex("""var (.*?) = \[(.*?)\];""")
        val matches = pattern.findAll(javascriptCode)

        val variableArrays: Map<String, List<String>> = matches.associate { matchResult ->
            val variableName = matchResult.groupValues[1]
            val urls = matchResult.groupValues[2]
                .split(",")
                .map { it.trim('\'', '"', ' ', '\n', '\r') }
            variableName to urls
        }
        val streamSourcesList = mutableListOf<StreamSource>()
        val rawStreamSourceUrls = mutableListOf<String>()
        streamSourcesList.addAll(
            variableArrays.values.parallelMap { urls ->

                runCatching {
                    val streamUrl = episodeNumber?.let { urls[it - 1] } ?: return@parallelMap null
                    if (rawStreamSourceUrls.contains(streamUrl)) return@parallelMap null
                    rawStreamSourceUrls.add(streamUrl)
                    when {
                        streamUrl.contains("sendvid") -> {
                            SendVidExtractor(client).getVideoFromUrl(streamUrl, headers)
                        }
                        streamUrl.contains("sibnet") -> {
                            SibNetExtractor(client).getVideoFromUrl(streamUrl, headers)
                        }
                        streamUrl.contains("myvi") -> {
                            MyViExtractor(client).getVideoFromUrl(streamUrl, headers)
                        }
                        streamUrl.contains("anime-sama") -> {
                            listOf(StreamSource(streamUrl, "AnimeSama"))
                        }
                        /*streamUrl.contains("vk.com") -> {
                            VkExtractor(client).getVideosFromUrl(streamUrl)
                        }*/
                        else -> null
                    }
                }.getOrNull()
            }.filterNotNull().flatten(),
        )

        return streamSourcesList.sort()
    }

    override fun episodeRequest(url: String): Request {
        episodeNumber = url.substringAfter("?number=").toInt()
        return GET(baseUrl + url.substringBeforeLast("?"), headers)
    }

    override fun fetchEpisode(url: String): Observable<List<StreamSource>> {
        return client.newCall(episodeRequest(url))
            .asCancelableObservable {
                it
            }.map { response ->
                val document = response.asJsoup()
                val episodeScriptFile =
                    document.selectFirst("script[src^=episodes.js?filever]")?.attr("src")
                val episodeScriptRequest =
                    GET("${baseUrl + url.substringBeforeLast("?")}/$episodeScriptFile", headers)
                val res = client.newCall(episodeScriptRequest).execute()
                episodeSourcesParse(res)
            }
    }
}