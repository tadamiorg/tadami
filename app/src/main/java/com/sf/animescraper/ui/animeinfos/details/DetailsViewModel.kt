package com.sf.animescraper.ui.animeinfos.details

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.animescraper.data.interactors.AnimeWithEpisodesInteractor
import com.sf.animescraper.data.interactors.UpdateAnimeInteractor
import com.sf.animescraper.domain.anime.Anime
import com.sf.animescraper.domain.episode.Episode
import com.sf.animescraper.network.api.online.AnimeSource
import com.sf.animescraper.network.requests.okhttp.HttpError
import com.sf.animescraper.ui.animeinfos.details.episodes.EpisodeItem
import com.sf.animescraper.ui.tabs.animesources.AnimeSourcesManager
import com.sf.animescraper.ui.utils.addOrRemove
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.await
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DetailsViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sourcesManager: AnimeSourcesManager = Injekt.get()
    private val updateAnimeInteractor: UpdateAnimeInteractor = Injekt.get()
    private val animeWithEpisodesInteractor: AnimeWithEpisodesInteractor = Injekt.get()

    private val animeId: Long = checkNotNull(savedStateHandle["animeId"])
    private val sourceId: String = checkNotNull(savedStateHandle["sourceId"])

    val source: AnimeSource = checkNotNull(sourcesManager.getExtensionById(sourceId))

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val selectedEpisodesIds: HashSet<Long> = HashSet()

    // Loaders for details and episodes

    private var _detailsRefreshing = MutableStateFlow(false)
    private val detailsRefreshing = _detailsRefreshing.asStateFlow()

    private var _episodesRefreshing = MutableStateFlow(false)
    private val episodesRefreshing = _episodesRefreshing.asStateFlow()

    val isRefreshing: StateFlow<Boolean> =
        combine(episodesRefreshing, detailsRefreshing) { values ->
            values.any { it }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            animeWithEpisodesInteractor.subscribe(animeId).collectLatest { (anime, episodes) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        details = anime,
                        episodes = episodes.sortedBy {
                            it.sourceOrder
                        }.toEpisodeItems()
                    )
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val anime = animeWithEpisodesInteractor.awaitAnime(animeId)
            val episodes = animeWithEpisodesInteractor.awaitEpisodes(animeId)

            try {
                if (!anime.initialized) {
                    _detailsRefreshing.update { true }
                    fetchAnimeDetailsFromSource(anime)
                }

                if (episodes.isEmpty()) {
                    _episodesRefreshing.update { true }
                    fetchEpisodesFromSource(anime)
                }
            } catch (e: HttpError) {
                when (e) {
                    is HttpError.Failure -> {
                        Log.e(
                            "Anime details",
                            "Anime details could not be retrieved. Error code : ${e.statusCode}"
                        )
                    }
                    else -> {
                        Log.d("Unknown error", e.toString(), e)
                    }
                }

                _episodesRefreshing.update { false }
                _detailsRefreshing.update { false }
            }

        }
    }

    private suspend fun fetchAnimeDetailsFromSource(anime: Anime) {
        val networkDetails = source.fetchAnimeDetails(anime).singleOrError().await()
        updateAnimeInteractor.awaitUpdateFromSource(anime, networkDetails)
        _detailsRefreshing.update { false }

    }

    private suspend fun fetchEpisodesFromSource(anime: Anime) {
        val networkEpisodes = source.fetchEpisodesList(anime).singleOrError().await()
        updateAnimeInteractor.awaitEpisodesSyncFromSource(anime, networkEpisodes)
        _episodesRefreshing.update { false }
    }

    fun onRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _episodesRefreshing.update { true }
            _detailsRefreshing.update { true }
            val anime = animeWithEpisodesInteractor.awaitAnime(animeId)
            fetchAnimeDetailsFromSource(anime)
            fetchEpisodesFromSource(anime)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch(Dispatchers.IO) {
            val anime = animeWithEpisodesInteractor.awaitAnime(animeId)
            updateAnimeInteractor.updateFavorite(anime, !anime.favorite)
        }
    }

    // Action Mode Functions

    fun setSeenStatus(){
        viewModelScope.launch(Dispatchers.IO) {
            selectedEpisodesIds.forEach {id ->
                val found = uiState.value.episodes.find { it.episode.id == id}?.episode
                if(found != null){
                    updateAnimeInteractor.awaitSeenUpdate(found,true)
                }
            }
            toggleAllSelectedEpisodes(false)
            selectedEpisodesIds.clear()
        }
    }

    fun setSeenStatusDown(){
        viewModelScope.launch(Dispatchers.IO) {
            if(selectedEpisodesIds.size > 1) return@launch

            val selectedEpisodeId = selectedEpisodesIds.first()
            val selectedEp = uiState.value.episodes.indexOfFirst { it.episode.id == selectedEpisodeId }

            if(selectedEp<0) return@launch

            val listSize = uiState.value.episodes.size

            val underEps = uiState.value.episodes.slice(selectedEp+1 until listSize)

            underEps.forEach {under ->
                updateAnimeInteractor.awaitSeenUpdate(under.episode,true)
            }

            toggleAllSelectedEpisodes(false)
            selectedEpisodesIds.clear()
        }
    }

    fun setUnseenStatus(){
        viewModelScope.launch(Dispatchers.IO) {
            selectedEpisodesIds.forEach {id ->
                val found = uiState.value.episodes.find { it.episode.id == id}?.episode
                if(found != null){
                    updateAnimeInteractor.awaitSeenUpdate(found,false)
                }
            }
            toggleAllSelectedEpisodes(false)
            selectedEpisodesIds.clear()
        }
    }

    fun toggleSelectedEpisode(episodeItem: EpisodeItem, selected: Boolean) {
        val newEpisodes = uiState.value.episodes.toMutableList().apply {
            val selectedIndex = this.indexOfFirst { it.episode.id == episodeItem.episode.id }
            if (selectedIndex < 0) return@apply

            val selectedItem = get(selectedIndex)

            set(selectedIndex, selectedItem.copy(selected = selected))

            selectedEpisodesIds.addOrRemove(episodeItem.episode.id, selected)

        }

        _uiState.update { currentState ->
            currentState.copy(
                episodes = newEpisodes
            )
        }
    }


    fun inverseSelectedEpisodes() {
        _uiState.update { currentState ->
            currentState.copy(
                episodes = currentState.episodes.map {
                    selectedEpisodesIds.addOrRemove(it.episode.id, !it.selected)
                    it.copy(selected = !it.selected)
                }
            )
        }
    }

    fun toggleAllSelectedEpisodes(selected: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                episodes = currentState.episodes.map {
                    selectedEpisodesIds.addOrRemove(it.episode.id, selected)
                    it.copy(selected = selected)
                }
            )
        }
    }
    private fun List<Episode>.toEpisodeItems(): List<EpisodeItem> {
        return this.map {
            EpisodeItem(
                it,
                it.id in selectedEpisodesIds
            )
        }
    }
}