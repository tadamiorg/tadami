package com.sf.animescraper.animesources.sources.en.animeheaven

import com.sf.animescraper.network.requests.okhttp.GET
import com.sf.animescraper.network.scraping.AnimeSource
import com.sf.animescraper.network.scraping.dto.crypto.StreamSource
import com.sf.animescraper.network.scraping.dto.details.AnimeDetails
import com.sf.animescraper.network.scraping.dto.details.DetailsEpisode
import com.sf.animescraper.network.scraping.dto.search.Anime
import com.sf.animescraper.network.scraping.dto.search.AnimeFilterList
import com.sf.animescraper.utils.Lang
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL

class AnimeHeaven : AnimeSource("AnimeHeaven") {
    override val name: String = "AnimeHeaven"
    override val baseUrl: String = "https://animeheaven.ru/"
    override val lang: Lang = Lang.ENGLISH

    override val client: OkHttpClient = network.cloudflareClient

    override fun latestSelector(): String = "div.iepbox div.iep"

    override fun latestAnimeFromElement(element: Element): Anime {
        val anime : Anime = Anime.create()
        val imgRef = element.select("div.ieppic a").first()
        anime.title = element.select("div.iepcon .cona").first()!!.text()
        anime.setUrlWithoutDomain(URL(imgRef?.attr("href")).path)
        anime.image = imgRef!!.select("img").first()?.attr("src")
        anime.episode = element.select("div.iepcon .iepst2r").first()?.text()
        return anime
    }

    override fun latestAnimesRequest(page: Int): Request {
        return GET("${baseUrl}latest-update/$page",headers)
    }

    override fun latestAnimeNextPageSelector(): String? = null

    override fun searchSelector(): String {
        return ""
    }

    override fun searchAnimeFromElement(element: Element): Anime {
        val anime : Anime = Anime.create()
        return anime
    }

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        return GET(baseUrl,headers)
    }

    override fun searchAnimeNextPageSelector(): String? = null

    // Details

    override fun animeDetailsParse(document: Document): AnimeDetails {
        val details = AnimeDetails.create()
        val detailsBody = document.selectFirst("div.anime_info_body")
        if(detailsBody != null) {
            val infosTypes = detailsBody.select("p.type")
            details.title = detailsBody.selectFirst("h1")?.text() ?: ""
            details.thumbnail_url = detailsBody.selectFirst("img")?.attr("src")
            details.description = infosTypes[1].ownText()
            details.genre = infosTypes[2].select("a").map {
                it.text().replace(",","").trim()
            }
            details.release = infosTypes[3].ownText()
            details.status = infosTypes[4].selectFirst("a")?.text()
        }
        return details


    }

    override fun episodesSelector(): String {
       return ""
    }

    override fun episodeFromElement(element: Element): DetailsEpisode {
        return DetailsEpisode(null,"caca","caca",null,false)
    }

    override fun streamSourcesSelector(): String {
        return ""
    }

    override fun streamSourcesFromElement(element: Element): List<StreamSource> {
        return emptyList<StreamSource>()
    }

    override fun animeDetailsRequest(anime : Anime): Request {
        return GET(baseUrl+anime.url,headers)
    }

}