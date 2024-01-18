package com.sf.tadami.ui.animeinfos.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.interactors.anime.AnimeWithEpisodesInteractor
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.ui.components.data.EpisodeItem
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesManager
import com.sf.tadami.ui.utils.addOrRemove
import com.sf.tadami.ui.utils.awaitSingleOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    val source = sourcesManager.getExtensionById(sourceId)

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

            if (!anime.initialized) {
                _detailsRefreshing.update { true }
                fetchAnimeDetailsFromSource(anime)
            }

            if (episodes.isEmpty()) {
                _episodesRefreshing.update { true }
                fetchEpisodesFromSource(anime)
            }
        }
    }

    // Update anime details functions

    private suspend fun fetchAnimeDetailsFromSource(anime: Anime) {
        val networkDetails = source.fetchAnimeDetails(anime)
            .awaitSingleOrNull { _detailsRefreshing.update { false } }
        networkDetails?.let {
            updateAnimeInteractor.awaitUpdateFromSource(anime, it)
            _detailsRefreshing.update { false }
        }
    }

    private suspend fun fetchEpisodesFromSource(anime: Anime, manualFetch : Boolean = false) {
        val networkEpisodes = source.fetchEpisodesList(anime)
            .awaitSingleOrNull { _episodesRefreshing.update { false } }
        networkEpisodes?.let {
            updateAnimeInteractor.awaitEpisodesSyncFromSource(anime, networkEpisodes,source,manualFetch)
            _episodesRefreshing.update { false }
        }
    }

    fun onRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _episodesRefreshing.update { true }
            _detailsRefreshing.update { true }
            val anime = animeWithEpisodesInteractor.awaitAnime(animeId)
            fetchAnimeDetailsFromSource(anime)
            fetchEpisodesFromSource(anime,true)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch(Dispatchers.IO) {
            val anime = animeWithEpisodesInteractor.awaitAnime(animeId)
            updateAnimeInteractor.updateLibraryAnime(anime, !anime.favorite)
        }
    }

    fun setEpisodeFlags(flags : Long){
        viewModelScope.launch(Dispatchers.IO) {
            val anime = animeWithEpisodesInteractor.awaitAnime(animeId)
            updateAnimeInteractor.updateAnimeEpisodeFlags(anime,flags)
        }
    }

    // Action Mode Functions

    fun setSeenStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            updateAnimeInteractor.awaitSeenEpisodeUpdate(selectedEpisodesIds, true)
            toggleAllSelectedEpisodes(false)
        }
    }

    fun setSeenStatusDown() {
        viewModelScope.launch(Dispatchers.IO) {
            if (selectedEpisodesIds.size > 1) return@launch

            val selectedEpisodeId = selectedEpisodesIds.first()
            val selectedEp =
                uiState.value.episodes.indexOfFirst { it.episode.id == selectedEpisodeId }

            if (selectedEp < 0) return@launch

            val listSize = uiState.value.episodes.size

            val underEps = uiState.value.episodes.slice(selectedEp + 1 until listSize)
                .mapTo(mutableSetOf()) { it.episode.id }

            toggleAllSelectedEpisodes(false)
            updateAnimeInteractor.awaitSeenEpisodeUpdate(underEps, true)
        }
    }

    fun setUnseenStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            updateAnimeInteractor.awaitSeenEpisodeUpdate(selectedEpisodesIds, false)
            toggleAllSelectedEpisodes(false)
        }
    }

    fun toggleSelectedEpisode(episodeItem: EpisodeItem, selected: Boolean) {
        val newEpisodes = uiState.value.episodes.toMutableList().apply {
            val selectedIndex =
                uiState.value.episodes.indexOfFirst { it.episode.id == episodeItem.episode.id }
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