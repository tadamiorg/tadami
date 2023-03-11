package com.sf.animescraper.ui.shared

import androidx.lifecycle.ViewModel
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.network.api.online.AnimeSource
import com.sf.animescraper.ui.tabs.animesources.AnimeSourcesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SharedViewModel : ViewModel() {

    val animeSourcesManager : AnimeSourcesManager = Injekt.get()

    private val _source = MutableStateFlow<AnimeSource?>(null)
    val source = _source.asStateFlow()

    private val _anime = MutableStateFlow<Anime?>(null)
    val anime = _anime.asStateFlow()

    fun setAnimeSource(sourceId : String){
        _source.update { animeSourcesManager.getExtensionById(sourceId) }
    }

    fun setAnime(newAnime : Anime){
        _anime.update { newAnime }
    }
}