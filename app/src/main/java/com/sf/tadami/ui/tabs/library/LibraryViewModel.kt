package com.sf.tadami.ui.tabs.library

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.data.interactors.library.LibraryInteractor
import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateWorker
import com.sf.tadami.ui.components.data.LibraryItem
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

    private val _initLoaded : MutableStateFlow<Boolean> = MutableStateFlow(false)
    val initLoaded = _initLoaded.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO){
            libraryInteractor.subscribe().collectLatest { libraryList ->
                Log.e("Library",libraryList.toString())
                _libraryList.update { libraryList.toLibraryItems() }
                if(!_initLoaded.value){
                    _initLoaded.update { true }
                }
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
    fun refreshLibrary(context: Context): Boolean {
        toggleRefreshIndicator()
        return LibraryUpdateWorker.startNow(context)
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