package com.sf.animescraper.ui.shared

import androidx.lifecycle.ViewModel
import com.sf.animescraper.animesources.sources.en.animeheaven.AnimeHeaven
import com.sf.animescraper.animesources.sources.en.gogoanime.GogoAnime
import com.sf.animescraper.network.scraping.AnimeSource
import com.sf.animescraper.network.scraping.dto.search.Anime
import com.sf.animescraper.ui.tabs.animesources.AnimeSourcesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SharedViewModel : ViewModel() {

    val animeSourcesManager : AnimeSourcesManager = Injekt.get()

    private val _uiState = MutableStateFlow(SharedUiState())
    val uiState : StateFlow<SharedUiState> = _uiState.asStateFlow()

    fun setAnimeSource(sourceId : String){
        _uiState.update { currentState ->
            currentState.copy(source = animeSourcesManager.getExtensionById(sourceId))
        }
    }

    fun selectAnime(selectedAnime : Anime){
        _uiState.update { currentState ->
            currentState.copy(selectedAnime = selectedAnime)
        }
    }
}