package com.sf.tadami.ui.tabs.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.R
import com.sf.tadami.data.interactors.LibraryInteractor
import com.sf.tadami.data.interactors.UpdateAnimeInteractor
import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateWorker
import com.sf.tadami.ui.components.data.LibraryItem
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.addOrRemove
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryViewModel : ViewModel() {
    private val libraryInteractor: LibraryInteractor = Injekt.get()
    private val updateAnimeInteractor : UpdateAnimeInteractor = Injekt.get()

    private val _libraryList : MutableStateFlow<List<LibraryItem>> = MutableStateFlow(emptyList())
    val libraryList = _libraryList.asStateFlow()

    private val _searchFilter : MutableStateFlow<String> = MutableStateFlow("")
    val searchFilter = _searchFilter.asStateFlow()

    private val _isRefreshing : MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val selectedIds : HashSet<Long> = HashSet()

    init {
        viewModelScope.launch(Dispatchers.IO){
            libraryInteractor.subscribe().collectLatest { libraryList ->
                _libraryList.update { libraryList.toLibraryItems() }
            }
        }
    }

    fun updateSearchFilter(value : String){
        _searchFilter.update {value}
    }

    private fun toggleRefreshIndicator(){
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.update { true }
            delay(500)
            _isRefreshing.update { false }
        }
    }
    fun refreshLibrary(context : Context){
        toggleRefreshIndicator()
        val started = LibraryUpdateWorker.startNow(context)
        viewModelScope.launch {
            val msgRes = if (started) context.getString(R.string.update_starting) else context.getString(R.string.update_running)
            UiToasts.showToast(msgRes)
        }
    }

    fun toggleSelected(libraryItem: LibraryItem, selected: Boolean) {
        val newLibraryItems = libraryList.value.toMutableList().apply {
            val selectedIndex = this.indexOfFirst { it.anime.id == libraryItem.anime.id }
            if (selectedIndex < 0) return@apply

            val selectedItem = get(selectedIndex)

            set(selectedIndex, selectedItem.copy(selected = selected))

            selectedIds.addOrRemove(libraryItem.anime.id, selected)

        }
        _libraryList.update{ newLibraryItems }
    }

    fun inverseSelected() {
        _libraryList.update { currentState ->
            currentState.map {
                selectedIds.addOrRemove(it.anime.id, !it.selected)
                it.copy(selected = !it.selected)
            }
        }
    }

    fun toggleAllSelected(selected: Boolean) {
        _libraryList.update { currentState ->
            currentState.map {
                selectedIds.addOrRemove(it.anime.id, selected)
                it.copy(selected = selected)
            }
        }
    }

    fun setSeenStatus(status : Boolean){
        viewModelScope.launch(Dispatchers.IO) {
            updateAnimeInteractor.awaitSeenAnimeUpdate(selectedIds, status)
            toggleAllSelected(false)
        }
    }
    fun unFavorite(){
        viewModelScope.launch(Dispatchers.IO) {
            updateAnimeInteractor.updateLibrary(selectedIds,false)
            toggleAllSelected(false)
        }
    }

    private fun List<LibraryAnime>.toLibraryItems(): List<LibraryItem> {
        return this.map {
            LibraryItem(
                it,
                it.id in selectedIds
            )
        }
    }
}