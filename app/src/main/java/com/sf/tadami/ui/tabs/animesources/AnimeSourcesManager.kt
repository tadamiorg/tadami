package com.sf.tadami.ui.tabs.animesources

import com.sf.tadami.animesources.sources.en.gogoanime.GogoAnime
import com.sf.tadami.animesources.sources.fr.animesama.AnimeSama
import com.sf.tadami.animesources.sources.fr.vostfree.VostFree
import com.sf.tadami.network.api.online.AnimeCatalogueSource
import com.sf.tadami.network.api.online.StubSource

class AnimeSourcesManager {

    private val extensions = listOf(
        GogoAnime(),
        AnimeSama(),
        VostFree()
    )

    val animeExtensions : Map<String,AnimeCatalogueSource> = extensions.associateBy { it.id }

    fun getExtensionsByLanguage() : Map<String, MutableList<AnimeCatalogueSource>> = animeExtensions.values
        .fold(mutableMapOf()) { langMap, animeSource ->
            langMap.getOrPut(animeSource.lang.name) { mutableListOf() }.add(animeSource)
            langMap
        }

    fun getExtensionById(id: String?): AnimeCatalogueSource {
        return animeExtensions[id] ?: StubSource(id ?: "Unknown")
    }
}