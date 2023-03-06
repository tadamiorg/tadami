package com.sf.animescraper.network.scraping.dto.search

import com.sf.animescraper.network.scraping.AnimeSource
import kotlin.properties.Delegates

class AnimeImpl : Anime {
    override lateinit var title: String
    override lateinit var url: String
    override var image: String? = null

    // For Favorites Animes
    override var source: AnimeSource? = null

    // For Recent or Searched Anime
    override var episode: String? = null
    override var released: String? = null
    override fun toString(): String {
        return "title : $title,url : $url"
    }

}
