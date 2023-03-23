package com.sf.tadami.network.api.online


import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.network.requests.okhttp.GET
import com.sf.tadami.network.api.model.AnimeFilterList
import com.sf.tadami.utils.Lang
import okhttp3.Request


abstract class AnimeSource(val id : String) : AnimeSourceBase() {

    abstract val name : String

    abstract val baseUrl : String

    abstract val lang : Lang

    open val supportSearch : Boolean = true

    open val supportRecent : Boolean = true


    override fun getIconRes(): Int? = null

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