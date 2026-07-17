package com.sf.tadami.ui.animeinfos.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.interactors.anime.AnimeWithEpisodesInteractor
import com.sf.tadami.data.interactors.anime.GetAnime
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.source.AnimeCatalogueSource
import com.sf.tadami.source.model.SSeason
import com.sf.tadami.ui.components.data.EpisodeItem
import com.sf.tadami.ui.discover.migrate.MigrateHelperState
import com.sf.tadami.ui.tabs.browse.SourceManager
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

    private val sourcesManager: SourceManager = Injekt.get()
    private val updateAnimeInteractor: UpdateAnimeInteractor = Injekt.get()
    private val animeWithEpisodesInteractor: AnimeWithEpisodesInteractor = Injekt.get()
    private val getAnime: GetAnime = Injekt.get()

    private val animeId: Long = checkNotNull(savedStateHandle["animeId"])
    private val sourceId: Long = checkNotNull(savedStateHandle["sourceId"])

    val source = sourcesManager.getOrStub(sourceId)

    private val hasSeasons: Boolean = (source as? AnimeCatalogueSource)?.hasSeasons ?: false

    private val _uiState = MutableStateFlow(DetailsUiState(hasSeasons = hasSeasons))
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

    private val _migrateHelperState = MutableStateFlow(MigrateHelperState())
    val migrateHelperState = _migrateHelperState.asStateFlow()

    init {
        val migrateIdString : String? = savedStateHandle["migrationId"]
        val migrateId : Long? = migrateIdString?.toLongOrNull()
        if(migrateId != null){
            viewModelScope.launch {
                val anime = getAnime.await(migrateId)!!
                _migrateHelperState.update {
                    it.copy(
                        oldAnime = anime
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            animeWithEpisodesInteractor.subscribe(animeId).collectLatest { (anime, episodes) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        details = anime,
                        episodes = episodes.sortedWith(
                            compareBy({ it.seasonNumber ?: 0f }, { it.sourceOrder })
                        ).toEpisodeItems()
                    )
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val anime = animeWithEpisodesInteractor.awaitAnime(animeId)
            val episodes = animeWithEpisodesInteractor.awaitEpisodes(animeId)

            _migrateHelperState.update {
                it.copy(
                    newAnime = anime
                )
            }

            if (!anime.initialized) {
                _detailsRefreshing.update { true }
                fetchAnimeDetailsFromSource(anime)
            }

            if (hasSeasons) {
                _episodesRefreshing.update { true }
                fetchSeasonsFromSource(anime)
            } else if (episodes.isEmpty()) {
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

    // Seasons (compatible sources only)

    private suspend fun fetchSeasonsFromSource(anime: Anime, manualFetch: Boolean = false) {
        val seasons = source.fetchSeasonsList(anime)
            .awaitSingleOrNull { _episodesRefreshing.update { false } }
        if (seasons == null) {
            _episodesRefreshing.update { false }
            return
        }
        val current = uiState.value.selectedSeason
        val selected = when {
            current != null && seasons.any { it.name == current.name } ->
                seasons.first { it.name == current.name }
            else -> pickDefaultSeason(seasons, uiState.value.episodes)
        }
        _uiState.update { it.copy(seasons = seasons, selectedSeason = selected) }
        if (selected != null) {
            loadSeasonEpisodes(anime, selected, manualFetch)
        } else {
            _episodesRefreshing.update { false }
        }
    }

    private fun pickDefaultSeason(seasons: List<SSeason>, episodes: List<EpisodeItem>): SSeason? {
        val firstUnseenSeasonName = episodes
            .sortedWith(compareBy({ it.episode.seasonNumber ?: 0f }, { it.episode.sourceOrder }))
            .firstOrNull { !it.episode.seen }?.episode?.seasonName
        return seasons.firstOrNull { it.name == firstUnseenSeasonName } ?: seasons.firstOrNull()
    }

    private suspend fun loadSeasonEpisodes(anime: Anime, season: SSeason, manualFetch: Boolean = false) {
        val alreadyLoaded = uiState.value.episodes.any { it.episode.seasonName == season.name }
        if (alreadyLoaded && !manualFetch) {
            _episodesRefreshing.update { false }
            return
        }
        _episodesRefreshing.update { true }
        val networkEpisodes = source.fetchEpisodesList(anime.copy(url = season.url))
            .awaitSingleOrNull { _episodesRefreshing.update { false } }
        if (networkEpisodes != null) {
            updateAnimeInteractor.awaitEpisodesSyncFromSource(
                anime = anime,
                remoteEpisodes = networkEpisodes,
                source = source,
                manualFetch = manualFetch,
                season = season
            )
        }
        _episodesRefreshing.update { false }
    }

    fun onSeasonSelected(season: SSeason) {
        if (season.name == uiState.value.selectedSeason?.name) return
        _uiState.update { it.copy(selectedSeason = season) }
        viewModelScope.launch(Dispatchers.IO) {
            val anime = animeWithEpisodesInteractor.awaitAnime(animeId)
            loadSeasonEpisodes(anime, season)
        }
    }

    fun onRefresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _episodesRefreshing.update { true }
            _detailsRefreshing.update { true }
            val anime = animeWithEpisodesInteractor.awaitAnime(animeId)
            fetchAnimeDetailsFromSource(anime)
            if (hasSeasons) {
                fetchSeasonsFromSource(anime, manualFetch = true)
            } else {
                fetchEpisodesFromSource(anime,true)
            }
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