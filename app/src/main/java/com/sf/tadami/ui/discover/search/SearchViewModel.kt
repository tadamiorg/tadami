package com.sf.tadami.ui.discover.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.interactors.anime.GetAnime
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.toDomainAnime
import com.sf.tadami.source.StubSource
import com.sf.tadami.source.model.AnimeFilterList
import com.sf.tadami.source.online.AnimeHttpSource
import com.sf.tadami.ui.discover.migrate.MigrateHelperState
import com.sf.tadami.ui.tabs.browse.SourceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SearchViewModel(
    stateHandle: SavedStateHandle
) : ViewModel() {
    private val animeRepository: AnimeRepository = Injekt.get()
    private val sourcesManager: SourceManager = Injekt.get()
    private val getAnime: GetAnime = Injekt.get()

    private val sourceId: Long = checkNotNull(stateHandle["sourceId"])
    val source by lazy {
        val s = sourcesManager.getOrStub(sourceId)
        if (s is StubSource) throw Exception("Not installed : $sourceId")
        s as AnimeHttpSource
    }

    private val filtersList = source.getFilterList()
    private val _sourceFilters = MutableStateFlow(filtersList)

    val sourceFilters: StateFlow<AnimeFilterList> = _sourceFilters.asStateFlow()

    private val _queryState = MutableStateFlow(QueryUiState(query = stateHandle["initialQuery"] ?: ""))
    val queryState: StateFlow<QueryUiState> = _queryState.asStateFlow()

    private val _migrateHelperState = MutableStateFlow(MigrateHelperState())
    val migrateHelperState = _migrateHelperState.asStateFlow()

    private fun getPager(): Flow<PagingData<Anime>> {
        return animeRepository.getSearchPager(
            source.id,
            queryState.value.query,
            sourceFilters.value
        )
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

    init {
        val migrateIdString : String? = stateHandle["migrationId"]
        val migrateId : Long? = migrateIdString?.toLongOrNull()
        if(migrateId != null){
            viewModelScope.launch {
                val anime = getAnime.await(migrateId)!!
                _migrateHelperState.update {
                    it.copy(oldAnime = anime)
                }
            }
        }

        if (queryState.value.query.isNotEmpty()) {
            _queryState.update {
                it.copy(fromGlobalSearch = true)
            }
            resetData()
        }
    }

    fun updateQuery(value: String) {
        _queryState.update { it.copy(query = value) }
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
    fun setClickedAnime(anime: Anime?){
        _migrateHelperState.update { it.copy(newAnime = anime) }
    }
}

data class QueryUiState(
    var query: String = "",
    var fromGlobalSearch: Boolean = false,
)