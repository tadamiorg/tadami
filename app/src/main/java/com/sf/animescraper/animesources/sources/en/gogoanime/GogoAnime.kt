package com.sf.animescraper.animesources.sources.en.gogoanime

import com.sf.animescraper.animesources.extractors.DoodExtractor
import com.sf.animescraper.animesources.extractors.StreamSBExtractor
import com.sf.animescraper.animesources.sources.en.gogoanime.extractors.GogoCdnExtractor
import com.sf.animescraper.network.requests.okhttp.GET
import com.sf.animescraper.network.requests.okhttp.asObservable
import com.sf.animescraper.network.requests.utils.asJsoup
import com.sf.animescraper.network.scraping.AnimeSource
import com.sf.animescraper.network.scraping.dto.crypto.StreamSource
import com.sf.animescraper.network.scraping.dto.details.AnimeDetails
import com.sf.animescraper.network.scraping.dto.details.DetailsEpisode
import com.sf.animescraper.network.scraping.dto.search.Anime
import com.sf.animescraper.network.scraping.dto.search.AnimeFilter
import com.sf.animescraper.network.scraping.dto.search.AnimeFilterList
import com.sf.animescraper.utils.Lang
import io.reactivex.rxjava3.core.Observable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy

class GogoAnime : AnimeSource("GogoAnime") {

    override val name: String = "GogoAnime"

    override val baseUrl: String = "https://www1.gogoanime.bid"

    override val lang: Lang = Lang.ENGLISH

    private val ajaxBaseUrl: String = "https://ajax.gogo-load.com/ajax"

    override val client: OkHttpClient = network.cloudflareClient

    private val json: Json by injectLazy()

    // Latest

    override fun latestSelector(): String {
        return "ul.items li"
    }

    override fun latestAnimeNextPageSelector(): String =
        "ul.pagination-list li:last-child:not(.selected)"

    override fun latestAnimeFromElement(element: Element): Anime {
        val anime: Anime = Anime.create()
        val imgRef = element.select("div.img a").first()
        anime.title = element.select("p.name").first()!!.text()
        anime.url = getDetailsURL(imgRef?.select("img")?.first()?.attr("src"))
        anime.image = imgRef!!.select("img").first()?.attr("src")
        anime.episode = element.select("p.episode").first()?.text()?.trim()?.split(" ")?.last()
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

    override fun searchAnimeFromElement(element: Element): Anime {
        val anime: Anime = Anime.create()
        anime.title = element.attr("title")
        anime.image = element.select("img").attr("src")
        anime.setUrlWithoutDomain(element.attr("href"))
        return anime
    }

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val filterList = if (filters.isEmpty()) getFilterList() else filters
        val genreFilter = filterList.find { it is GenreList } as GenreList
        val statusFilter = filterList.find { it is StatusList } as StatusList
        val yearsFilter = filterList.find { it is YearsList } as YearsList
        val sortFilter = filterList.find { it is SortList } as SortList
        val seasonsFilters = filterList.find { it is SeasonList } as SeasonList

        var searchRequest = "$baseUrl/filter.html?keyword=$query"

        if (genreFilter.state.isNotEmpty()) {
            genreFilter.state.forEach {
                if (it.state) {
                    searchRequest += "&genre[]=${(it as MultiChoices).value}"
                }
            }
        }

        if (seasonsFilters.state.isNotEmpty()) {
            seasonsFilters.state.forEach {
                if (it.state) {
                    searchRequest += "&season[]=${(it as MultiChoices).value}"
                }
            }
        }

        if (yearsFilter.state.isNotEmpty()) {
            yearsFilter.state.forEach {
                if (it.state) {
                    searchRequest += "&year[]=${(it as MultiChoices).value}"
                }
            }
        }

        if (statusFilter.state.isNotEmpty()) {
            statusFilter.state.forEach {
                if (it.state) {
                    searchRequest += "&status[]=${(it as MultiChoices).value}"
                }
            }
        }

        searchRequest += "&sort=${sortFilters()[sortFilter.state].second}"

        searchRequest += "&page=$page"

        return GET(searchRequest, headers)
    }

    // Details

    override fun animeDetailsParse(document: Document): AnimeDetails {
        val details = AnimeDetails.create()
        val detailsBody = document.selectFirst("div.anime_info_body")

        val infosTypes = detailsBody?.select("p.type")
        details.title = detailsBody?.selectFirst("h1")?.text() ?: ""
        details.url = "/category/${document.location().split("/").last()}"
        details.thumbnail_url = detailsBody?.selectFirst("img")?.attr("src")
        details.description = infosTypes?.get(1)?.ownText()
        details.genre = infosTypes?.get(2)?.select("a")?.map {
            it.text().replace(",", "").trim()
        }
        details.release = infosTypes?.get(3)?.ownText()
        details.status = infosTypes?.get(4)?.selectFirst("a")?.text()

        return details
    }

    // Episodes List

    override fun episodesSelector(): String = "li > a"

    override fun episodeFromElement(element: Element): DetailsEpisode {
        val url = getUrlWithoutDomain(baseUrl + element.attr("href").substringAfter(" "))
        val ep = element.selectFirst("div.name")?.ownText()?.substringAfter(" ")
        val name = "Episode $ep"
        val episodeNumber = ep?.toFloat()
        return DetailsEpisode(url = url, name = name, seen = false)
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

    override fun fetchEpisodesList(anime: Anime): Observable<List<DetailsEpisode>> {
        val episodesListRequest = client.newCall(episodesRequest(anime))
            .asObservable()
            .map { response ->

                getGogoEpisodesRequest(response)
            }
        return episodesListRequest.flatMap { request ->
            client.newCall(request)
                .asObservable().map { response ->
                    episodesParse(response)
                }
        }
    }

    // Episode Source Stream

    fun List<StreamSource>.sort(): List<StreamSource> {
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
        val extractor = GogoCdnExtractor(network.client, json)
        val streamSourcesList = mutableListOf<StreamSource>()
        // GogoCdn:
        document.select("div.anime_muti_link > ul > li.vidcdn > a")
            .firstOrNull()?.attr("data-video")
            ?.let { streamSourcesList.addAll(extractor.videosFromUrl("https:$it")) }
        // Vidstreaming:
        document.select("div.anime_muti_link > ul > li.anime > a")
            .firstOrNull()?.attr("data-video")
            ?.let { streamSourcesList.addAll(extractor.videosFromUrl("https:$it")) }
        // Doodstream mirror:
        document.select("div.anime_muti_link > ul > li.doodstream > a")
            .firstOrNull()?.attr("data-video")
            ?.let { streamSourcesList.addAll(DoodExtractor(client).videosFromUrl(it)) }
        // StreamSB mirror:
        document.select("div.anime_muti_link > ul > li.streamsb > a")
            .firstOrNull()?.attr("data-video")
            ?.let { streamSourcesList.addAll(StreamSBExtractor(client).videosFromUrl(it, headers)) }
        return streamSourcesList.sort()
    }

    // Filters

    override fun getFilterList(): AnimeFilterList = AnimeFilterList(
        SortList(sortFilters()),
        GenreList(genresFilters()),
        StatusList(statusFilters()),
        YearsList(yearsFilters()),
        SeasonList(seasonsFilters())
    )

    private class MultiChoices(title: String, val value: String) : AnimeFilter.CheckBox(title)

    private class GenreList(genres: MutableList<MultiChoices>) :
        AnimeFilter.CheckBoxGroup("Genres", genres)

    private class StatusList(status: MutableList<MultiChoices>) :
        AnimeFilter.CheckBoxGroup("Status", status)

    private class YearsList(years: MutableList<MultiChoices>) :
        AnimeFilter.CheckBoxGroup("Year", years)

    private class SeasonList(seasons: MutableList<MultiChoices>) :
        AnimeFilter.CheckBoxGroup("Season", seasons)

    private class SortList(values: Array<Pair<String, String>>) :
        AnimeFilter.Select("Sort", values.map { it.first }.toTypedArray())

    //REGEX : { name: ("[-\w\s]*"), value: ("[-\w\s]*"), },

    private fun sortFilters() = arrayOf(
        Pair("Name A-Z", "title_az"),
        Pair("Recently updated", "recently_updated"),
        Pair("Recently added", "recently_added"),
        Pair("Release date", "release_date")
    )

    private fun seasonsFilters() = arrayListOf(
        MultiChoices("Fall", "fall"),
        MultiChoices("Summer", "summer"),
        MultiChoices("Spring", "spring"),
        MultiChoices("Winter", "winter"),
    )

    private fun statusFilters() = arrayListOf(
        MultiChoices("Ongoing", "Ongoing"),
        MultiChoices("Not Yet Aired", "Upcoming"),
        MultiChoices("Completed", "Completed"),
    )

    private fun yearsFilters() = arrayListOf(
        MultiChoices("2023", "2023"),
        MultiChoices("2022", "2022"),
        MultiChoices("2021", "2021"),
        MultiChoices("2020", "2020"),
        MultiChoices("2019", "2019"),
        MultiChoices("2018", "2018"),
        MultiChoices("2017", "2017"),
        MultiChoices("2016", "2016"),
        MultiChoices("2015", "2015"),
        MultiChoices("2014", "2014"),
        MultiChoices("2013", "2013"),
        MultiChoices("2012", "2012"),
        MultiChoices("2011", "2011"),
        MultiChoices("2010", "2010"),
        MultiChoices("2009", "2009"),
        MultiChoices("2008", "2008"),
        MultiChoices("2007", "2007"),
        MultiChoices("2006", "2006"),
        MultiChoices("2005", "2005"),
        MultiChoices("2004", "2004"),
        MultiChoices("2003", "2003"),
        MultiChoices("2002", "2002"),
        MultiChoices("2001", "2001"),
        MultiChoices("2000", "2000"),
        MultiChoices("1999", "1999")
    )

    private fun genresFilters() = arrayListOf(
        MultiChoices("Action", "action"),
        MultiChoices("Adult Cast", "adult-cast"),
        MultiChoices("Adventure", "adventure"),
        MultiChoices("Anthropomorphic", "anthropomorphic"),
        MultiChoices("Avant Garde", "avant-garde"),
        MultiChoices("Boys Love", "shounen-ai"),
        MultiChoices("Cars", "cars"),
        MultiChoices("CGDCT", "cgdct"),
        MultiChoices("Childcare", "childcare"),
        MultiChoices("Comedy", "comedy"),
        MultiChoices("Comic", "comic"),
        MultiChoices("Crime", "crime"),
        MultiChoices("Crossdressing", "crossdressing"),
        MultiChoices("Delinquents", "delinquents"),
        MultiChoices("Dementia", "dementia"),
        MultiChoices("Demons", "demons"),
        MultiChoices("Detective", "detective"),
        MultiChoices("Drama", "drama"),
        MultiChoices("Dub", "dub"),
        MultiChoices("Ecchi", "ecchi"),
        MultiChoices("Erotica", "erotica"),
        MultiChoices("Family", "family"),
        MultiChoices("Fantasy", "fantasy"),
        MultiChoices("Gag Humor", "gag-humor"),
        MultiChoices("Game", "game"),
        MultiChoices("Gender Bender", "gender-bender"),
        MultiChoices("Gore", "gore"),
        MultiChoices("Gourmet", "gourmet"),
        MultiChoices("Harem", "harem"),
        MultiChoices("Hentai", "hentai"),
        MultiChoices("High Stakes Game", "high-stakes-game"),
        MultiChoices("Historical", "historical"),
        MultiChoices("Horror", "horror"),
        MultiChoices("Isekai", "isekai"),
        MultiChoices("Iyashikei", "iyashikei"),
        MultiChoices("Josei", "josei"),
        MultiChoices("Kids", "kids"),
        MultiChoices("Magic", "magic"),
        MultiChoices("Magical Sex Shift", "magical-sex-shift"),
        MultiChoices("Mahou Shoujo", "mahou-shoujo"),
        MultiChoices("Martial Arts", "martial-arts"),
        MultiChoices("Mecha", "mecha"),
        MultiChoices("Medical", "medical"),
        MultiChoices("Military", "military"),
        MultiChoices("Music", "music"),
        MultiChoices("Mystery", "mystery"),
        MultiChoices("Mythology", "mythology"),
        MultiChoices("Organized Crime", "organized-crime"),
        MultiChoices("Parody", "parody"),
        MultiChoices("Performing Arts", "performing-arts"),
        MultiChoices("Pets", "pets"),
        MultiChoices("Police", "police"),
        MultiChoices("Psychological", "psychological"),
        MultiChoices("Reincarnation", "reincarnation"),
        MultiChoices("Romance", "romance"),
        MultiChoices("Romantic Subtext", "romantic-subtext"),
        MultiChoices("Samurai", "samurai"),
        MultiChoices("School", "school"),
        MultiChoices("Sci-Fi", "sci-fi"),
        MultiChoices("Seinen", "seinen"),
        MultiChoices("Shoujo", "shoujo"),
        MultiChoices("Shoujo Ai", "shoujo-ai"),
        MultiChoices("Shounen", "shounen"),
        MultiChoices("Slice of Life", "slice-of-life"),
        MultiChoices("Space", "space"),
        MultiChoices("Sports", "sports"),
        MultiChoices("Strategy Game", "strategy-game"),
        MultiChoices("Super Power", "super-power"),
        MultiChoices("Supernatural", "supernatural"),
        MultiChoices("Suspense", "suspense"),
        MultiChoices("Team Sports", "team-sports"),
        MultiChoices("Thriller", "thriller"),
        MultiChoices("Time Travel", "time-travel"),
        MultiChoices("Vampire", "vampire"),
        MultiChoices("Work Life", "work-life"),
        MultiChoices("Workplace", "workplace"),
        MultiChoices("Yaoi", "yaoi"),
        MultiChoices("Yuri", "yuri")
    )
}