package com.sf.animescraper.ui.discover.recent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.sf.animescraper.data.anime.AnimeRepository
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.domain.anime.toDomainAnime
import com.sf.animescraper.network.api.online.AnimeSource
import com.sf.animescraper.ui.shared.SharedViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RecentViewModel(stateHandle: SavedStateHandle) : ViewModel() {

    private val sharedViewModel: SharedViewModel = Injekt.get()
    private val animeRepository: AnimeRepository = Injekt.get()

    private val sourceId: String? = stateHandle["sourceId"]

    val source = sharedViewModel.source.value as AnimeSource

    val animeList: Flow<PagingData<Anime>> = animeRepository.getLatestPager(source.id)
        .map { pagingData ->
            pagingData.map { sAnime ->
                animeRepository.insertNetworkToLocalAnime(sAnime.toDomainAnime(source.id))
            }
        }.cachedIn(viewModelScope)


    fun onAnimeClicked(anime: Anime) {
        sharedViewModel.setAnime(anime)
    }
}