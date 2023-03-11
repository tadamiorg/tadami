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
import com.sf.animescraper.ui.tabs.animesources.AnimeSourcesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RecentViewModel(stateHandle: SavedStateHandle) : ViewModel() {

    private val animeRepository: AnimeRepository = Injekt.get()
    private val sourcesManager: AnimeSourcesManager = Injekt.get()

    private val sourceId: String = checkNotNull(stateHandle["sourceId"])
    val source = checkNotNull(sourcesManager.getExtensionById(sourceId))

    val animeList: Flow<PagingData<Anime>> = animeRepository.getLatestPager(source.id)
        .map { pagingData ->
            pagingData.map { sAnime ->
                animeRepository.insertNetworkToLocalAnime(sAnime.toDomainAnime(source.id))
            }
        }.cachedIn(viewModelScope)
}