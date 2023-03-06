package com.sf.animescraper.ui.tabs.animesources

import com.sf.animescraper.animesources.sources.en.animeheaven.AnimeHeaven
import com.sf.animescraper.animesources.sources.en.gogoanime.GogoAnime
import com.sf.animescraper.network.scraping.AnimeSource

class AnimeSourcesManager{

    private val extensions = listOf(
        GogoAnime(),
        AnimeHeaven()
    )

    val animeExtensions = extensions.associateBy { it.id }

    fun getExtensionById(id : String): AnimeSource? {
        return animeExtensions[id]
    }
}