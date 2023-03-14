package com.sf.animescraper.ui.tabs.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.animescraper.data.interactors.FavoriteInteractor
import com.sf.animescraper.domain.anime.FavoriteAnime
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

    private val _favoriteList : MutableStateFlow<List<FavoriteAnime>> = MutableStateFlow(emptyList())
    val favoriteList = _favoriteList.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO){
            favoriteInteractor.subscribe().collectLatest { favoriteList ->
                _favoriteList.update { favoriteList }
            }
        }
    }
}