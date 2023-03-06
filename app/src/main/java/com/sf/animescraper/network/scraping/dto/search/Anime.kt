package com.sf.animescraper.network.scraping.dto.search

import com.sf.animescraper.network.scraping.AnimeSource

interface Anime{
    var title : String
    var url : String
    var image : String?

    // For Favorites Animes
    var source : AnimeSource?

    // For Recent or Searched Anime
    var episode : String?
    var released : String?

    companion object{
        fun create() : AnimeImpl {
            return AnimeImpl()
        }
    }
}