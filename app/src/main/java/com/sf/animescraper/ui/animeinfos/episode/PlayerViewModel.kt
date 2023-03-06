package com.sf.animescraper.ui.animeinfos.episode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.animescraper.network.requests.okhttp.Callback
import com.sf.animescraper.network.requests.utils.ObserverAS
import com.sf.animescraper.network.scraping.AnimeSource
import com.sf.animescraper.network.scraping.dto.crypto.StreamSource
import com.sf.animescraper.network.scraping.dto.search.Anime
import com.sf.animescraper.ui.shared.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class PlayerViewModel(sharedViewModel: SharedViewModel = Injekt.get()) : ViewModel() {

    private val _uiState = MutableStateFlow(EpisodeUiState())
    val uiState: StateFlow<EpisodeUiState> = _uiState.asStateFlow()

    private val _currentEpisodeIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val currentEpisodeIndex: StateFlow<Int> = _currentEpisodeIndex.asStateFlow()

    private val source = sharedViewModel.uiState.value.source as AnimeSource

    fun setCurrentEpisodeIndex(index: Int) {
        _currentEpisodeIndex.update { index }
    }

    fun selectSource(source: StreamSource) {
        _uiState.update { currentState ->
            currentState.copy(selectedSource = source)
        }
    }

    fun selectEpisode(
        url: String,
        callback: Callback<List<StreamSource>>? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            source.fetchEpisode(url).subscribe(object : ObserverAS<List<StreamSource>>(callback) {
                override fun onNext(data: List<StreamSource>) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            rawUrl = url,
                            selectedSource = data.firstOrNull(),
                            availableSources = data
                        )
                    }
                    super.onNext(data)
                }
            })
        }
    }
}