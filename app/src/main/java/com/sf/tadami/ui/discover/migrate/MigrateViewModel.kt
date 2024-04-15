package com.sf.tadami.ui.discover.migrate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.interactors.anime.GetAnime
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.ui.discover.globalSearch.GlobalSearchViewModelBase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateViewModel(
    stateHandle: SavedStateHandle,
) : GlobalSearchViewModelBase(stateHandle) {

    val animeId: Long = checkNotNull(stateHandle["animeId"])
    private val getAnime: GetAnime = Injekt.get()

    private val _helperState = MutableStateFlow(MigrateHelperState())
    val helperState = _helperState.asStateFlow()

    init {
        viewModelScope.launch {
            val anime = getAnime.await(animeId)!!
            _helperState.update { it.copy(oldAnime = anime) }
            _uiState.update {
                it.copy(
                    fromSourceId = anime.source,
                    searchQuery = anime.title
                )
            }
            search()
        }
    }
    fun setClickedAnime(anime: Anime?){
        _helperState.update { it.copy(newAnime = anime) }
    }
}

data class MigrateHelperState(
    var oldAnime : Anime? = null,
    var newAnime : Anime? = null
)