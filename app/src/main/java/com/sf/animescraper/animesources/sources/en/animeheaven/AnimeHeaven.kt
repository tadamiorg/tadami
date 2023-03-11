package com.sf.animescraper.animesources.sources.en.animeheaven

import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.network.requests.okhttp.GET
import com.sf.animescraper.network.api.online.AnimeSource
import com.sf.animescraper.network.api.model.StreamSource
import com.sf.animescraper.network.api.model.SAnime
import com.sf.animescraper.network.api.model.AnimeFilterList
import com.sf.animescraper.network.api.model.SEpisode
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

    override fun latestAnimeFromElement(element: Element): SAnime {
        val anime : SAnime = SAnime.create()
        val imgRef = element.select("div.ieppic a").first()
        anime.title = element.select("div.iepcon .cona").first()!!.text()
        anime.setUrlWithoutDomain(URL(imgRef?.attr("href")).path)
        anime.thumbnailUrl = imgRef!!.select("img").first()?.attr("src")
        return anime
    }

    override fun latestAnimesRequest(page: Int): Request {
        return GET("${baseUrl}latest-update/$page",headers)
    }

    override fun latestAnimeNextPageSelector(): String? = null

    override fun searchSelector(): String {
        return ""
    }

    override fun searchAnimeFromElement(element: Element): SAnime {
        return SAnime.create()
    }

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        return GET(baseUrl,headers)
    }

    override fun searchAnimeNextPageSelector(): String? = null

    // Details

    override fun animeDetailsParse(document: Document): SAnime {
        return SAnime.create()
    }

    override fun episodesSelector(): String {
       return ""
    }

    override fun episodeFromElement(element: Element): SEpisode {
        return SEpisode.create()
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