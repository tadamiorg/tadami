package com.sf.tadami.ui.tabs.updates

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.data.interactors.updates.GetUpdatesInteractor
import com.sf.tadami.domain.updates.UpdatesWithRelations
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateWorker
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.ui.utils.addOrRemove
import com.sf.tadami.utils.editPreferences
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.launchIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZonedDateTime

class UpdatesViewModel : ViewModel() {
    private val getUpdatesInteractor : GetUpdatesInteractor = Injekt.get()
    private val updateAnimeInteractor : UpdateAnimeInteractor = Injekt.get()
    private val dataStore : DataStore<Preferences> = Injekt.get()

    private var _uiState = MutableStateFlow(UpdatesUiState())
    val uiState = _uiState.asStateFlow()

    private val _isRefreshing : MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val selectedEpisodesIds: HashSet<Long> = HashSet()

    init {
        viewModelScope.launchIO {
            // Set date limit for recent chapters
            val limit = ZonedDateTime.now().minusMonths(3).toInstant()

            getUpdatesInteractor.subscribe(limit).distinctUntilChanged().collectLatest {updates->
                _uiState.update {
                    it.copy(
                        items = updates.toUpdateItems()
                    )
                }
            }
        }
    }

    fun resetNewUpdatesCount(){
        viewModelScope.launchIO {
            val libraryPreferences = dataStore.getPreferencesGroup(LibraryPreferences)
            dataStore.editPreferences(libraryPreferences.copy(newUpdatesCount = 0),
                LibraryPreferences
            )
        }
    }

    private fun toggleRefreshIndicator(){
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.update { true }
            delay(500)
            _isRefreshing.update { false }
        }
    }

    fun updateLibrary(): Boolean {
        toggleRefreshIndicator()
        return LibraryUpdateWorker.startNow(Injekt.get<Application>())
    }

    fun setUpdatesSeenStatus() {
        viewModelScope.launchIO {
            updateAnimeInteractor.awaitSeenEpisodeUpdate(
                seen = true,
                episodesIds = selectedEpisodesIds,
            )
            toggleAllSelectedItems(false)
        }
    }

    fun setUpdatesUnSeenStatus() {
        viewModelScope.launchIO {
            updateAnimeInteractor.awaitSeenEpisodeUpdate(
                seen = false,
                episodesIds = selectedEpisodesIds,
            )
            toggleAllSelectedItems(false)
        }
    }

    fun toggleSelectedItems(updateItem: UpdatesItem, selected: Boolean) {
        val newItems = _uiState.value.items!!.toMutableList().apply {
            val selectedIndex = indexOfFirst { it.update.episodeId == updateItem.update.episodeId }
            if (selectedIndex < 0) return@apply

            val selectedItem = get(selectedIndex)

            if (selectedItem.selected == selected) return@apply

            set(selectedIndex, selectedItem.copy(selected = selected))

            selectedEpisodesIds.addOrRemove(updateItem.update.episodeId, selected)

        }

        _uiState.update { currentState ->
            currentState.copy(
                items = newItems.toList()
            )
        }
    }


    fun inverseSelectedItems() {
        _uiState.update { currentState ->
            currentState.copy(
                items = currentState.items!!.map {
                    selectedEpisodesIds.addOrRemove(it.update.episodeId, !it.selected)
                    it.copy(selected = !it.selected)
                }
            )
        }
    }

    fun toggleAllSelectedItems(selected: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                items = currentState.items!!.map {
                    selectedEpisodesIds.addOrRemove(it.update.episodeId, selected)
                    it.copy(selected = selected)
                }
            )
        }
    }

    private fun List<UpdatesWithRelations>.toUpdateItems(): List<UpdatesItem> {
        return this
            .map { update ->
                UpdatesItem(
                    update = update,
                    selected = update.episodeId in selectedEpisodesIds,
                )
            }
            .toList()
    }
}

