package com.sf.tadami.ui.discover.globalSearch

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.domain.anime.toDomainAnime
import com.sf.tadami.network.api.online.AnimeCatalogueSource
import com.sf.tadami.ui.components.globalSearch.GlobalSearchItemResult
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesManager
import com.sf.tadami.ui.tabs.settings.externalpreferences.source.SourcesPreferences
import com.sf.tadami.ui.utils.awaitSingleOrError
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class GlobalSearchViewModel() : ViewModel() {
    private val animeRepository: AnimeRepository = Injekt.get()
    private val sourcesManager: AnimeSourcesManager = Injekt.get()
    private val dataStore : DataStore<Preferences> = Injekt.get()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _animesBySource = MutableStateFlow(GlobalSearchUiState(emptyMap()))
    val animesBySource = _animesBySource.asStateFlow()

    fun search(newQuery : String) {
        if(newQuery.isEmpty()) return

        val sourcesPrefs = runBlocking {
            dataStore.getPreferencesGroup(SourcesPreferences)
        }

        val filteredExtensions = sourcesManager.animeExtensions.filter { (_,source) ->
            source.lang.getRes().toString() in sourcesPrefs.enabledLanguages && source.id !in sourcesPrefs.hiddenSources
        }

        _animesBySource.update {
            val loadingItems = filteredExtensions.map { (_, source) ->
                source
            }.associateWith { GlobalSearchItemResult.Loading }
            GlobalSearchUiState(loadingItems)
        }

        viewModelScope.launch(Dispatchers.IO) {
            filteredExtensions.map { (_, source) ->
                async {
                    try {
                        val page = source.fetchSearch(1, _query.value, source.getFilterList(),true)
                            .awaitSingleOrError()
                        val titles = page.animes.map { sAnime ->
                            animeRepository.insertNetworkToLocalAnime(sAnime.toDomainAnime(source.id))
                        }
                        _animesBySource.update { currentState ->
                            val mutableItems = currentState.items.toMutableMap()
                            mutableItems[source] = GlobalSearchItemResult.Success(titles)
                            currentState.copy(items = mutableItems)
                        }

                    } catch (e: Exception) {
                        _animesBySource.update { currentState ->
                            val mutableItems = currentState.items.toMutableMap()
                            mutableItems[source] = GlobalSearchItemResult.Error(e)
                            currentState.copy(items = mutableItems)
                        }
                    }

                }
            }.awaitAll()
        }
    }

    fun updateQuery(value: String) {
        _query.update { value }
    }
}

data class GlobalSearchUiState(
    val items: Map<AnimeCatalogueSource, GlobalSearchItemResult>
)