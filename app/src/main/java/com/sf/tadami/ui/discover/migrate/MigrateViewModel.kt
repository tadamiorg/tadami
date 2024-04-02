package com.sf.tadami.ui.discover.migrate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.interactors.anime.GetAnime
import com.sf.tadami.domain.anime.toDomainAnime
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.ui.components.globalSearch.GlobalSearchItemResult
import com.sf.tadami.ui.tabs.browse.SourceManager
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

class MigrateViewModel(
    stateHandle: SavedStateHandle,
) : ViewModel() {

    private val animedId: Long = checkNotNull(stateHandle["animedId"])
    private val getAnime: GetAnime = Injekt.get()
    private val sourceManager: SourceManager = Injekt.get()
    private val dataStore : DataStore<Preferences> = Injekt.get()
    private val animeRepository: AnimeRepository = Injekt.get()

    private val sourcesPrefs = runBlocking {
        dataStore.getPreferencesGroup(SourcesPreferences)
    }

    private val _uiState = MutableStateFlow(MigrateSearchUiState())
    val uiState = _uiState.asStateFlow()

    private val _query : MutableStateFlow<String> = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val filteredExtensions = sourceManager.getCatalogueSources().filter { source ->
        source.lang.name in sourcesPrefs.enabledLanguages && "${source.id}" !in sourcesPrefs.hiddenSources
    }.sortedWith(
        compareBy { it.id != uiState.value.fromSourceId },
    )

    private val loadingItems = filteredExtensions.associateWith { GlobalSearchItemResult.Loading }

    init {
        viewModelScope.launch {
            val anime = getAnime.await(animedId)!!
            _query.update {
                anime.title
            }
            _uiState.update {
                it.copy(
                    fromSourceId = anime.source,
                )
            }
            search(anime.title)
        }
    }

    fun search(newQuery : String) {
        if(newQuery.isEmpty()) return

        _uiState.update {
            it.copy(
                items = loadingItems
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            filteredExtensions.map { source ->
                async {
                    try {
                        val page = source.fetchSearch(1, _query.value, source.getFilterList(),true)
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
}