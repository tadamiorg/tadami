package com.sf.animescraper.ui.tabs.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.animescraper.data.interactors.FavoriteInteractor
import com.sf.animescraper.data.interactors.UpdateAnimeInteractor
import com.sf.animescraper.domain.anime.FavoriteAnime
import com.sf.animescraper.domain.anime.toAnime
import com.sf.animescraper.ui.components.data.FavoriteItem
import com.sf.animescraper.ui.utils.addOrRemove
import kotlinx.coroutines.Dispatchers
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

    private val selectedFavoriteIds : HashSet<Long> = HashSet()

    init {
        viewModelScope.launch(Dispatchers.IO){
            favoriteInteractor.subscribe().collectLatest { favoriteList ->
                _favoriteList.update { favoriteList.toFavoriteItems() }
            }
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
            val selected = favoriteList.value.filter {
                it.anime.id in selectedFavoriteIds
            }
            selected.forEach {
                updateAnimeInteractor.awaitSeenAnimeUpdate(it.anime.id, status)
            }
            selectedFavoriteIds.clear()
            toggleAllSelectedFavorites(false)
        }
    }
    fun unFavorite(){
        viewModelScope.launch(Dispatchers.IO) {
            val selected = favoriteList.value.filter {
                it.anime.id in selectedFavoriteIds
            }
            selected.forEach {
                updateAnimeInteractor.updateFavorite(it.anime.toAnime(),false)
            }
            selectedFavoriteIds.clear()
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