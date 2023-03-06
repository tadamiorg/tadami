package com.sf.animescraper.ui.discover.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.animescraper.network.requests.okhttp.Callback
import com.sf.animescraper.network.requests.utils.ObserverAS
import com.sf.animescraper.network.scraping.AnimeSource
import com.sf.animescraper.network.scraping.AnimesPage
import com.sf.animescraper.network.scraping.dto.search.Anime
import com.sf.animescraper.network.scraping.dto.search.AnimeFilterList
import com.sf.animescraper.ui.shared.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SearchViewModel(private val sharedViewModel: SharedViewModel = Injekt.get()) : ViewModel()
{
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val source = sharedViewModel.uiState.value.source as AnimeSource

    private val filtersList = source.getFilterList()
    private val _sourceFilters = MutableStateFlow(filtersList)

    val sourceFilters: StateFlow<AnimeFilterList> = _sourceFilters.asStateFlow()

    private var page: Int = 1

    private val _query = MutableStateFlow("")
    private val query : StateFlow<String> = _query.asStateFlow()

    fun updateQuery(value : String){
        _query.update { value }
    }

    fun updateFilters(updatedFilters : AnimeFilterList){
        _sourceFilters.value = updatedFilters
    }

    fun resetFilters(){
        updateFilters(source.getFilterList())
    }

    fun getSearch(callback: Callback<AnimesPage>? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            source.fetchSearch(page++,query.value,_sourceFilters.value).subscribe(object : ObserverAS<AnimesPage>(callback) {
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
            currentState.copy(animeList = listOf(), hasNextPage = true)
        }
        page = 1
    }
}