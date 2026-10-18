package com.sf.tadami.ui.tabs.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.data.interactors.category.GetCategories
import com.sf.tadami.data.interactors.library.LibraryInteractor
import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.domain.anime.toAnime
import com.sf.tadami.domain.category.Category
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateWorker
import com.sf.tadami.preferences.app.BasePreferences
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.ui.components.data.LibraryItem
import com.sf.tadami.ui.utils.addOrRemove
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class LibraryViewModel : ViewModel() {
    private val libraryInteractor: LibraryInteractor = Injekt.get()
    private val getCategories: GetCategories = Injekt.get()
    private val updateAnimeInteractor : UpdateAnimeInteractor = Injekt.get()
    private val downloadManager : TadamiDownloadManager = Injekt.get()
    private val dataStore : DataStore<Preferences> = Injekt.get()

    private val _libraryList : MutableStateFlow<List<LibraryItem>> = MutableStateFlow(emptyList())
    val libraryList = _libraryList.asStateFlow()

    // The user's categories, without the Default one
    private val _categories : MutableStateFlow<List<Category>> = MutableStateFlow(emptyList())
    val categories = _categories.asStateFlow()

    // The category of the tab shown, the last one used at start
    private val _activeCategoryId : MutableStateFlow<Long> = MutableStateFlow(Category.UNCATEGORIZED_ID)
    val activeCategoryId = _activeCategoryId.asStateFlow()

    private val _searchFilter : MutableStateFlow<String> = MutableStateFlow("")
    val searchFilter = _searchFilter.asStateFlow()

    // The anime with a downloaded episode while the downloaded only mode is on, null when it is off
    private val _downloadedIds : MutableStateFlow<Set<Long>?> = MutableStateFlow(null)
    val downloadedIds = _downloadedIds.asStateFlow()

    private val _isRefreshing : MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val selectedIds : HashSet<Long> = HashSet()

    private val _initLoaded : MutableStateFlow<Boolean> = MutableStateFlow(false)
    val initLoaded = _initLoaded.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO){
            // Read before the first load, the tabs open on it
            _activeCategoryId.update {
                dataStore.data.first()[LibraryPreferences.LAST_USED_CATEGORY] ?: Category.UNCATEGORIZED_ID
            }
            combine(
                libraryInteractor.subscribe(),
                getCategories.subscribe(),
            ) { libraryList, categories ->
                libraryList to categories
            }.collectLatest { (libraryList, categories) ->
                _libraryList.update { libraryList.toLibraryItems() }
                _categories.update { categories }
                if(!_initLoaded.value){
                    _initLoaded.update { true }
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            // The downloads change without the library, the cache tells when
            combine(
                libraryInteractor.subscribe(),
                dataStore.getPreferencesGroupAsFlow(BasePreferences).map { it.downloadedOnly }.distinctUntilChanged(),
                downloadManager.cacheChanges,
            ) { libraryList, downloadedOnly, _ ->
                if (downloadedOnly) {
                    libraryList.filter { downloadManager.getDownloadCount(it.toAnime()) > 0 }.mapTo(HashSet()) { it.id }
                } else {
                    null
                }
            }.collectLatest { ids ->
                _downloadedIds.update { ids }
            }
        }
    }

    fun updateSearchFilter(value : String){
        _searchFilter.update {value}
    }

    fun setActiveCategory(categoryId: Long) {
        if (_activeCategoryId.value == categoryId) return
        _activeCategoryId.update { categoryId }
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { it[LibraryPreferences.LAST_USED_CATEGORY] = categoryId }
        }
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

    // On the tab shown, [pageIds], the anime selected in the other tabs stay selected
    fun inverseSelected(displayedIds: Set<Long>, pageIds: Set<Long>) {
        updateDisplayedSelection(displayedIds, pageIds) { !it.selected }
    }

    fun selectAllDisplayed(displayedIds: Set<Long>, pageIds: Set<Long>) {
        updateDisplayedSelection(displayedIds, pageIds) { true }
    }

    // Called when the search or the filters change: an anime no longer displayed can't stay selected
    fun keepDisplayedSelected(displayedIds: Set<Long>) {
        updateDisplayedSelection(displayedIds) { it.selected }
    }

    private fun updateDisplayedSelection(
        displayedIds: Set<Long>,
        pageIds: Set<Long> = displayedIds,
        selected: (LibraryItem) -> Boolean
    ) {
        _libraryList.update { currentState ->
            currentState.selectDisplayed(displayedIds, pageIds, selected).also { items ->
                selectedIds.clear()
                items.filter { it.selected }.mapTo(selectedIds) { it.anime.id }
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

// Select all and invert act on the displayed items of the tab shown, [pageIds]: the hidden ones end up
// unselected, the ones displayed in other tabs keep their selection
internal fun List<LibraryItem>.selectDisplayed(
    displayedIds: Set<Long>,
    pageIds: Set<Long> = displayedIds,
    selected: (LibraryItem) -> Boolean
): List<LibraryItem> {
    return map {
        val isSelected = when (it.anime.id) {
            in pageIds -> selected(it)
            in displayedIds -> it.selected
            else -> false
        }
        it.copy(selected = isSelected)
    }
}