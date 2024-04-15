package com.sf.tadami.ui.discover.globalSearch

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.domain.anime.toDomainAnime
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.ui.components.globalSearch.GlobalSearchItemResult
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.utils.awaitSingleOrError
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get


data class GlobalSearchUiState(
    val fromSourceId: Long? = null,
    val items: Map<AnimeCatalogueSource, GlobalSearchItemResult> = emptyMap(),
    val searchQuery: String = "",
) {
    val progress: Int = items.count { it.value !is GlobalSearchItemResult.Loading }
    val total: Int = items.size
}

open class GlobalSearchViewModelBase(
    stateHandle: SavedStateHandle
) : ViewModel() {
    private val sourceManager: SourceManager = Injekt.get()
    private val dataStore: DataStore<Preferences> = Injekt.get()
    private val animeRepository: AnimeRepository = Injekt.get()

    private var lastQuery : String = ""

    val _uiState = MutableStateFlow(GlobalSearchUiState(searchQuery = stateHandle["initialQuery"] ?: "") )
    val uiState = _uiState.asStateFlow()

    val sourcesPrefs = runBlocking {
        dataStore.getPreferencesGroup(SourcesPreferences)
    }

    open fun getEnabledSources(): List<AnimeCatalogueSource> {
        return sourceManager.getCatalogueSources()
            .filter { source ->
                source.lang.name in sourcesPrefs.enabledLanguages && "${source.id}" !in sourcesPrefs.hiddenSources
            }
            .sortedWith(
                compareBy { it.id != uiState.value.fromSourceId },
            )
    }

    fun search() {
        val query = uiState.value.searchQuery
        if(lastQuery == query) return

        lastQuery = query

        if (query.isEmpty()) return

        val enabledSources = getEnabledSources()

        _uiState.update {
            it.copy(
                items = enabledSources.associateWith { GlobalSearchItemResult.Loading }
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            enabledSources.map { source ->
                async {
                    try {
                        val page = source.fetchSearch(
                            1,
                            query,
                            source.getFilterList(),
                            true
                        )
                            .awaitSingleOrError()
                        val titles = page.animes.map { sAnime ->
                            animeRepository.insertNetworkToLocalAnime(sAnime.toDomainAnime(source.id))
                        }
                        _uiState.update { currentState ->
                            val mutableItems = currentState.items.toMutableMap()
                            mutableItems[source] = GlobalSearchItemResult.Success(titles)
                            currentState.copy(items = mutableItems)
                        }

                    } catch (e: Exception) {
                        _uiState.update { currentState ->
                            val mutableItems = currentState.items.toMutableMap()
                            mutableItems[source] = GlobalSearchItemResult.Error(e)
                            currentState.copy(items = mutableItems)
                        }
                    }

                }
            }.awaitAll()
        }
    }

    fun updateSearchQuery(newValue: String) {
        _uiState.update { it.copy(searchQuery = newValue) }
    }
}