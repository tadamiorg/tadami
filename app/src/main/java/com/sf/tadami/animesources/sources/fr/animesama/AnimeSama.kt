package com.sf.tadami.animesources.sources.fr.animesama

import android.text.Html
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.animesources.extractors.SendvidExtractor
import com.sf.tadami.animesources.extractors.SibnetExtractor
import com.sf.tadami.animesources.extractors.VkExtractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.network.api.model.AnimeFilterList
import com.sf.tadami.network.api.model.SAnime
import com.sf.tadami.network.api.model.SEpisode
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.api.online.AnimesPage
import com.sf.tadami.network.api.online.ConfigurableParsedHttpAnimeSource
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.requests.okhttp.POST
import com.sf.tadami.network.requests.okhttp.asCancelableObservable
import com.sf.tadami.network.requests.utils.asJsoup
import com.sf.tadami.ui.tabs.settings.components.PreferenceScreen
import com.sf.tadami.ui.utils.capFirstLetter
import com.sf.tadami.ui.utils.parallelMap
import com.sf.tadami.utils.Lang
import io.reactivex.rxjava3.core.Observable
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class AnimeSama : ConfigurableParsedHttpAnimeSource<AnimeSamaPreferences>(AnimeSamaPreferences) {

    override val id: String = "AnimeSama"

    override val name: String = "AnimeSama"

    override val baseUrl: String = preferences.baseUrl

    override val lang: Lang = Lang.FRENCH

    override val client: OkHttpClient = network.cloudflareClient

    override fun getIconRes(): Int {
        return R.drawable.animesama
    }

    private var episodeNumber: Int? = null

    override val supportRecent = false

    override fun getPreferenceScreen(navController: NavHostController): PreferenceScreen {
        return AnimeSamaPreferencesScreen(navController,dataStore)
    }

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
            .asCancelableObservable()
            .flatMap { response ->

                val document = response.asJsoup()

                val animeList = document.select(searchSelector()).map { element ->
                    searchAnimeFromElement(element)
                }

                val hasNextPage = searchAnimeNextPageSelector().let { selector ->
                    document.select(selector).first()
                } != null

                val pageRequests = Observable.fromIterable(animeList).flatMap { anime ->
                    client.newCall(GET("$baseUrl${anime.url}"))
                        .asCancelableObservable()
                        .map { response ->
                            val doc = response.asJsoup()
                            val seasonDiv =
                                doc.selectFirst("h2:contains(Anime) ~ div:has(script)")
                            val seasonScript =
                                seasonDiv?.selectFirst("script")?.data()?.trimIndent()
                            val seasonsMatches = seasonScript?.let {
                                Regex("""[^(/*)]panneauAnime\("(.*?)",\s*"(.*?)"\)""").findAll(it)
                            }

                            val kaiSeasonDiv =
                                doc.selectFirst("h2:contains(Anime Version Kai) ~ div:has(script)")
                            val kaiSeasonScript =
                                kaiSeasonDiv?.selectFirst("script")?.data()?.trimIndent()
                            val kaiSeasonsMatches = kaiSeasonScript?.let {
                                Regex("""[^(/*)]panneauAnime\("(.*?)",\s*"(.*?)"\)""").findAll(it)
                            }

                            val seasons = seasonsMatches?.map { matchResult ->
                                val (param1, param2) = matchResult.destructured
                                param1 to param2
                            }?.toMutableList()

                            val kaiSeasons = kaiSeasonsMatches?.map { matchResult ->
                                val (param1, param2) = matchResult.destructured
                                param1 to param2
                            }?.toList()

                            seasons?.addAll(kaiSeasons ?: emptyList())

                            seasons?.map { (seasonName, seasonUrl) ->
                                val animeSeason = SAnime.create()
                                animeSeason.url = "${anime.url}/$seasonUrl"
                                animeSeason.thumbnailUrl = anime.thumbnailUrl
                                animeSeason.title =
                                    "${parseSeason(seasonUrl).takeIf { it.isNotBlank() } ?: seasonName} - ${anime.title}"
                                animeSeason
                            } ?: emptyList<SAnime>()
                        }
                }.toList().toObservable()

                pageRequests.map { pages ->
                    val animeSeasons = pages.flatten()
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
        anime.setUrlWithoutDomain(element.selectFirst("a")!!.attr("href").removeSuffix("/"))
        return anime
    }


    override fun searchAnimeNextPageSelector(): String = "div#nav_pages a.bg-sky-900 ~ a"

    override fun animeDetailsRequest(anime: Anime): Request {
        return GET(baseUrl + anime.url + "/../..", headers)
    }

    override fun fetchAnimeDetails(anime: Anime): Observable<SAnime> {
        return client.newCall(animeDetailsRequest(anime))
            .asCancelableObservable()
            .map { response ->
                animeDetailsParse(response.asJsoup(), parseSeason(anime.url))
            }
    }

    private fun parseSeason(seasonUrl: String?): String {
        if (seasonUrl == null) return ""
        val pathSegments = seasonUrl.split("/").takeIf { it.size >= 2 }
        val seasonPath = pathSegments?.get(pathSegments.size - 2) ?: return ""

        val seasonRegex = Regex("""(\D+)(\d*)(\D*)""")
        val regexResults = seasonRegex.find(seasonPath) ?: return ""
        val firstPart = regexResults.groupValues.getOrNull(1) ?: return ""

        val secondPart = regexResults.groupValues.getOrNull(2)

        val thirdPart = regexResults.groupValues.getOrNull(3)

        var season = firstPart.capFirstLetter()
        if (secondPart != null) {
            season += " $secondPart"
        }
        if(thirdPart!=null && thirdPart== "hs"){
            season += " SF"
        }

        return season.trim()
    }

    private fun animeDetailsParse(document: Document, season: String): SAnime {
        val anime = SAnime.create()
        anime.title = "$season - " + document.selectFirst("#titreOeuvre")!!.text()
        anime.description =
            document.selectFirst("h2:contains(Synopsis) ~ p.text-sm.text-gray-400.mt-2")?.text()
        anime.genres = document.selectFirst("h2:contains(Genres) ~ a")?.text()?.trim()
            ?.split(Regex("""( - )|,"""))
        anime.thumbnailUrl = document.selectFirst("meta[itemprop=image]")?.attr("content")
        return anime
    }

    override fun animeDetailsParse(document: Document): SAnime = throw Exception("Not used")

    override fun episodesSelector(): String = throw Exception("Not used")

    override fun episodeFromElement(element: Element): SEpisode = throw Exception("Not used")

    private fun getEpisodesTrueNames(document: Document): List<Pair<String, List<String>>> {
        val resultList = mutableListOf<Pair<String, List<String>>>()
        val scriptRegex = Regex("""<script>([^<]*(?:(?!<\/script>)<[^<]*)*)<\/script>""")
        val scripts = scriptRegex.findAll(document.html())
        scripts.forEach { script ->
            val code = script.groupValues.getOrNull(1)?.substringAfter(">")
                ?.takeIf { !it.contains("#avOeuvre") && it.contains("resetListe();") }
            if (code != null) {
                val commentsRegex = Regex("""\/\*.*?\*\/""", RegexOption.DOT_MATCHES_ALL)
                val codeWithoutComments = code.replace(commentsRegex, "").trimIndent()
                val functionCalls =
                    codeWithoutComments
                        .substringAfter("resetListe();")
                        .substringBefore("});")
                        .substringBeforeLast(";")
                        .split(";")

                functionCalls.forEach { call ->
                    val functionName = call.substringBefore("(").trim()
                    val parameters = call.substringAfter("(").substringBefore(")")
                        .trim()
                        .split(Regex(""",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*${'$'})"""))
                        .map { it.removeSurrounding("\"").trim() }
                    resultList.add(functionName to parameters)
                }
                return resultList
            }
        }
        return resultList
    }

    private fun parseEpisodesTrueNames(
        names: List<Pair<String, List<String>>>,
        totalEpisodes: Int?
    ): List<String> {
        if (totalEpisodes == null) return emptyList()
        val episodesNames = mutableListOf<String>()
        try {
            names.forEach { (function, parameters) ->
                when (function) {
                    "newSPF" -> {
                        episodesNames.add(parameters[0])
                    }
                    "newSP" -> {
                        episodesNames.add("Episode ${parameters[0]}")
                    }
                    "creerListe" -> {
                        val debut = parameters[0].toInt()
                        val fin = parameters[1].toInt()
                        for (i in debut..fin) {
                            episodesNames.add("Episode $i")
                        }
                    }
                    "finirListe", "finirListeOP"-> {
                        val baseEpNumber = parameters[0].toInt()
                        for (i in 0 until (totalEpisodes-episodesNames.size)-1) {
                            episodesNames.add("Episode ${baseEpNumber+i}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
        return episodesNames
    }

    override fun fetchEpisodesList(anime: Anime): Observable<List<SEpisode>> {
        return client.newCall(episodesRequest(anime))
            .asCancelableObservable()
            .flatMap { response ->
                val document = response.asJsoup()
                val episodeScriptRequest = GET(baseUrl + anime.url + "/episodes.js", headers)
                client.newCall(episodeScriptRequest)
                    .asCancelableObservable()
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
                        val trueNames = try {
                            parseEpisodesTrueNames(
                                getEpisodesTrueNames(document),
                                variableWithMostLinks?.value
                            )
                        } catch (e : Exception){
                            emptyList()
                        }
                        for (i in 1..(variableWithMostLinks?.value?.minus(1) ?: 0)) {
                            val episode = SEpisode.create()
                            episode.episodeNumber = i.toFloat()
                            episode.name = if (trueNames.getOrNull(i - 1) != null) {
                                trueNames[i - 1]
                            } else {
                                "Episode ${i.toFloat()}"
                            }
                            episode.url = "${anime.url}?number=$i"
                            episodesList.add(episode)
                        }
                        episodesList.reversed()
                    }
            }
    }

    override fun episodeSourcesParse(response: Response): List<StreamSource> {

        val document: Document = response.asJsoup()
        val javascriptCode = Html.fromHtml(document.html(), Html.FROM_HTML_MODE_LEGACY).toString()
        val pattern = Regex("""(?<!\/\*)var (.*?) = \[(.*?)\];""")
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
                        streamUrl.contains("sendvid.com") -> {
                            SendvidExtractor(client, headers).videosFromUrl(streamUrl)
                        }
                        streamUrl.contains("sibnet.ru") -> {
                            SibnetExtractor(client).videosFromUrl(streamUrl)
                        }
                        streamUrl.contains("anime-sama.fr") -> {
                            listOf(StreamSource(streamUrl, "AnimeSama"))
                        }
                        streamUrl.contains("vk.") -> {
                            VkExtractor(client, headers).videosFromUrl(streamUrl)
                        }
                        else -> null
                    }
                }.getOrNull()
            }.filterNotNull().flatten(),
        )

        return streamSourcesList.sort()
    }

    override fun streamSourcesSelector(): String = throw Exception("Not used")

    override fun streamSourcesFromElement(element: Element): List<StreamSource> =
        throw Exception("Not used")

    override fun episodeRequest(url: String): Request {
        episodeNumber = url.substringAfter("?number=").toInt()
        return GET(baseUrl + url.substringBeforeLast("?")  +"/episodes.js", headers)
    }

    override fun List<StreamSource>.sort(): List<StreamSource> {
        val server = "AnimeSama"

        return this.sortedWith(
            compareBy { it.quality.contains(server) }
        ).reversed()
    }
}