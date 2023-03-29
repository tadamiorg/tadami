package com.sf.tadami.ui.tabs.favorites

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.R
import com.sf.tadami.data.interactors.FavoriteInteractor
import com.sf.tadami.data.interactors.UpdateAnimeInteractor
import com.sf.tadami.domain.anime.FavoriteAnime
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateWorker
import com.sf.tadami.ui.components.data.FavoriteItem
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

class FavoritesViewModel : ViewModel() {
    private val favoriteInteractor: FavoriteInteractor = Injekt.get()
    private val updateAnimeInteractor : UpdateAnimeInteractor = Injekt.get()

    private val _favoriteList : MutableStateFlow<List<FavoriteItem>> = MutableStateFlow(emptyList())
    val favoriteList = _favoriteList.asStateFlow()

    private val _searchFilter : MutableStateFlow<String> = MutableStateFlow("")
    val searchFilter = _searchFilter.asStateFlow()

    private val _isRefreshing : MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val selectedFavoriteIds : HashSet<Long> = HashSet()

    init {
        viewModelScope.launch(Dispatchers.IO){
            favoriteInteractor.subscribe().collectLatest { favoriteList ->
                _favoriteList.update { favoriteList.toFavoriteItems() }
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
    fun refreshAllFavorites(context : Context){
        toggleRefreshIndicator()
        val started = LibraryUpdateWorker.startNow(context)
        viewModelScope.launch {
            val msgRes = if (started) context.getString(R.string.update_starting) else context.getString(R.string.update_running)
            UiToasts.showToast(msgRes)
        }
    }

    fun toggleSelectedFavorite(favorite: FavoriteItem, selected: Boolean) {
        val newFavorites = favoriteList.value.toMutableList().apply {
            val selectedIndex = this.indexOfFirst { it.anime.id == favorite.anime.id }
            if (selectedIndex < 0) return@apply

            val selectedItem = get(selectedIndex)

            set(selectedIndex, selectedItem.copy(selected = selected))

            selectedFavoriteIds.addOrRemove(favorite.anime.id, selected)

        }
        _favoriteList.update{ newFavorites }
    }

    fun inverseSelectedFavorites() {
        _favoriteList.update { currentState ->
            currentState.map {
                selectedFavoriteIds.addOrRemove(it.anime.id, !it.selected)
                it.copy(selected = !it.selected)
            }
        }
    }

    fun toggleAllSelectedFavorites(selected: Boolean) {
        _favoriteList.update { currentState ->
            currentState.map {
                selectedFavoriteIds.addOrRemove(it.anime.id, selected)
                it.copy(selected = selected)
            }
        }
    }

    fun setSeenStatus(status : Boolean){
        viewModelScope.launch(Dispatchers.IO) {
            updateAnimeInteractor.awaitSeenAnimeUpdate(selectedFavoriteIds, status)
            toggleAllSelectedFavorites(false)
        }
    }
    fun unFavorite(){
        viewModelScope.launch(Dispatchers.IO) {
            updateAnimeInteractor.updateAllFavorite(selectedFavoriteIds,false)
            toggleAllSelectedFavorites(false)
        }
    }

    private fun List<FavoriteAnime>.toFavoriteItems(): List<FavoriteItem> {
        return this.map {
            FavoriteItem(
                it,
                it.id in selectedFavoriteIds
            )
        }
    }
}