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
import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.source.online.StubSource
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SearchViewModel(
    stateHandle: SavedStateHandle
) : ViewModel() {
    private val animeRepository: AnimeRepository = Injekt.get()
    private val sourcesManager: AnimeSourcesManager = Injekt.get()

    private val sourceId: Long = checkNotNull(stateHandle["sourceId"])
    val source by lazy {
        val s = sourcesManager.getExtensionById(sourceId)
        if(s is StubSource) throw Exception("Not installed : $sourceId")
        s
    }

    private val filtersList = source.getFilterList()
    private val _sourceFilters = MutableStateFlow(filtersList)

    val sourceFilters: StateFlow<AnimeFilterList> = _sourceFilters.asStateFlow()

    private val _query = MutableStateFlow(stateHandle["baseQuery"] ?: "")
    val query: StateFlow<String> = _query.asStateFlow()

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