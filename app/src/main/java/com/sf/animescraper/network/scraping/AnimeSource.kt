package com.sf.animescraper.network.scraping


import com.sf.animescraper.network.requests.okhttp.GET
import com.sf.animescraper.network.scraping.dto.search.Anime
import com.sf.animescraper.network.scraping.dto.search.AnimeFilterList
import com.sf.animescraper.utils.Lang
import okhttp3.Request


abstract class AnimeSource(val id : String) : AnimeSourceBase() {

    abstract val name : String

    abstract val baseUrl : String

    abstract val lang : Lang

    open val supportSearch : Boolean = true

    open val supportRecent : Boolean = true

    // Filters

    override fun getFilterList(): AnimeFilterList = AnimeFilterList()

    // Anime Details

    override fun animeDetailsRequest(anime: Anime): Request {
        return GET(baseUrl+anime.url,headers)
    }

    // Episodes

    override fun episodesRequest(anime: Anime): Request {
        return GET(baseUrl+anime.url,headers)
    }

    // Episode Sources

    override fun episodeRequest(url: String): Request {
        return GET(baseUrl+url,headers)
    }

}