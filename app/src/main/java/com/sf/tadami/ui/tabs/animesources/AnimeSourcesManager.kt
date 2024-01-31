package com.sf.tadami.ui.tabs.animesources

import com.sf.tadami.animesources.sources.en.gogoanime.GogoAnime
import com.sf.tadami.animesources.sources.fr.animesama.AnimeSama
import com.sf.tadami.animesources.sources.fr.vostfree.VostFree
import com.sf.tadami.source.online.AnimeCatalogueSource
import com.sf.tadami.source.online.StubSource

class AnimeSourcesManager {

    private val extensions = listOf(
        GogoAnime(),
        AnimeSama(),
        VostFree()
    )

    val animeExtensions : Map<Long, AnimeCatalogueSource> = extensions.associateBy { it.id }

    fun getExtensionsByLanguage() : Map<String, MutableList<AnimeCatalogueSource>> = animeExtensions.values
        .fold(mutableMapOf()) { langMap, animeSource ->
            langMap.getOrPut(animeSource.lang.name) { mutableListOf() }.add(animeSource)
            langMap
        }

    fun getExtensionById(id: Long?): AnimeCatalogueSource {
        return animeExtensions[id] ?: StubSource(id ?: -1)
    }
}