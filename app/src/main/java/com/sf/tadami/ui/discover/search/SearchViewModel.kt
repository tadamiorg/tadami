package com.sf.tadami.ui.discover.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.toDomainAnime
import com.sf.tadami.network.api.model.AnimeFilterList
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesManager
import kotlinx.coroutines.flow.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SearchViewModel(
    stateHandle: SavedStateHandle
) : ViewModel() {
    private val animeRepository: AnimeRepository = Injekt.get()
    private val sourcesManager: AnimeSourcesManager = Injekt.get()

    private val sourceId: String = checkNotNull(stateHandle["sourceId"])
    val source = checkNotNull(sourcesManager.getExtensionById(sourceId))

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

    fun resetData() {
        _animeList.update {
            getPager()
        }
    }
}