package com.sf.animescraper.ui.discover.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.sf.animescraper.data.anime.AnimeRepository
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.domain.anime.toDomainAnime
import com.sf.animescraper.network.api.model.AnimeFilterList
import com.sf.animescraper.network.api.online.AnimeSource
import com.sf.animescraper.ui.shared.SharedViewModel
import kotlinx.coroutines.flow.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SearchViewModel() : ViewModel() {
    private val sharedViewModel: SharedViewModel = Injekt.get()
    private val animeRepository: AnimeRepository = Injekt.get()

    val source = sharedViewModel.source.value as AnimeSource

    private val filtersList = source.getFilterList()
    private val _sourceFilters = MutableStateFlow(filtersList)

    val sourceFilters: StateFlow<AnimeFilterList> = _sourceFilters.asStateFlow()

    private val _query = MutableStateFlow("")
    private val query: StateFlow<String> = _query.asStateFlow()

    private fun getPager(): Flow<PagingData<Anime>> {
        return animeRepository.getSearchPager(source.id, query.value, sourceFilters.value)
            .map { pagingData ->
                pagingData.map { sAnime ->
                    animeRepository.insertNetworkToLocalAnime(sAnime.toDomainAnime(source.id))
                }
            }.cachedIn(viewModelScope)
    }

    private var _animeList = MutableStateFlow(
        getPager()
    )
    val animeList = _animeList.asStateFlow()

    fun updateQuery(value: String) {
        _query.update { value }
    }

    fun updateFilters(updatedFilters: AnimeFilterList) {
        _sourceFilters.value = updatedFilters
    }

    fun resetFilters() {
        updateFilters(source.getFilterList())
    }

    fun onAnimeClicked(anime: Anime) {
        sharedViewModel.setAnime(anime)
    }

    fun resetData() {
        _animeList.update {
            getPager()
        }
    }
}