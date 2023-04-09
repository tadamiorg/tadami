package com.sf.tadami.ui.tabs.animesources

import com.sf.tadami.animesources.sources.en.gogoanime.GogoAnime
import com.sf.tadami.animesources.sources.fr.vostfree.VostFree
import com.sf.tadami.network.api.online.AnimeSource

class AnimeSourcesManager{

    private val extensions = listOf(
        GogoAnime(),
        VostFree()
    )

    val animeExtensions = extensions.associateBy { it.id }

    fun getExtensionById(id : String): AnimeSource? {
        return animeExtensions[id]
    }
}