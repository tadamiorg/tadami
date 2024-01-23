package com.sf.tadami.ui.discover.recent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.toDomainAnime
import com.sf.tadami.source.online.StubSource
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RecentViewModel(stateHandle: SavedStateHandle) : ViewModel() {

    private val animeRepository: AnimeRepository = Injekt.get()
    private val sourcesManager: AnimeSourcesManager = Injekt.get()

    private val sourceId: String = checkNotNull(stateHandle["sourceId"])
    val source by lazy {
        val s = sourcesManager.getExtensionById(sourceId)
        if(s is StubSource) throw Exception("Not installed : $sourceId")
        s
    }

    val animeList: Flow<PagingData<Anime>> = animeRepository.getLatestPager(source.id)
        .map { pagingData ->
            pagingData.map { sAnime ->
                animeRepository.insertNetworkToLocalAnime(sAnime.toDomainAnime(source.id))
            }
        }.cachedIn(viewModelScope)
}