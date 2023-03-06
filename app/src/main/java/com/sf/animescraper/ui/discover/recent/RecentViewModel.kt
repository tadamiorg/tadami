package com.sf.animescraper.ui.discover.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.animescraper.network.requests.okhttp.Callback
import com.sf.animescraper.network.requests.utils.ObserverAS
import com.sf.animescraper.network.scraping.AnimeSource
import com.sf.animescraper.network.scraping.AnimesPage
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

class RecentViewModel(
    private val sharedViewModel: SharedViewModel = Injekt.get()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecentUiState())
    val uiState: StateFlow<RecentUiState> = _uiState.asStateFlow()

    val source = sharedViewModel.uiState.value.source as AnimeSource

    private var page: Int = 1

    fun getRecent(callback: Callback<AnimesPage>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            source.fetchLatest(page++).subscribe(object : ObserverAS<AnimesPage>(callback) {
                override fun onNext(data: AnimesPage) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            animeList = currentState.animeList + data.animes,
                            hasNextPage = data.hasNextPage
                        )
                    }
                    super.onNext(data)
                }
            })
        }
    }

    fun onAnimeClicked(anime: Anime) {
        sharedViewModel.selectAnime(anime)
    }

    fun resetData() {
        _uiState.update { currentState ->
            currentState.copy(animeList = listOf())
        }
        page = 1
    }
}