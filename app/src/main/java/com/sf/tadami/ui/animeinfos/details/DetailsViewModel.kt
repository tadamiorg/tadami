package com.sf.tadami.ui.animeinfos.details

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.data.interactors.anime.AnimeWithEpisodesInteractor
import com.sf.tadami.data.interactors.anime.GetAnime
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.data.interactors.category.GetCategories
import com.sf.tadami.data.interactors.category.SetAnimeCategories
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.toSAnime
import com.sf.tadami.domain.category.DefaultCategoryChoice
import com.sf.tadami.domain.category.defaultCategoryChoice
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.extension.util.callGuarded
import com.sf.tadami.preferences.app.BasePreferences
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.ui.animeinfos.details.episodes.DetailsDownloadAction
import com.sf.tadami.ui.animeinfos.details.episodes.EpisodeDownloadAction
import com.sf.tadami.ui.animeinfos.details.episodes.episodesToDownload
import com.sf.tadami.ui.category.categoryCheckboxes
import com.sf.tadami.ui.components.data.EpisodeItem
import com.sf.tadami.ui.discover.migrate.MigrateHelperState
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.utils.addOrRemove
import com.sf.tadami.ui.utils.awaitOrNull
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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
    private val downloadManager: TadamiDownloadManager = Injekt.get()
    private val dataStore: DataStore<Preferences> = Injekt.get()
    private val getCategories: GetCategories = Injekt.get()
    private val setAnimeCategories: SetAnimeCategories = Injekt.get()

    private val animeId: Long = checkNotNull(savedStateHandle["animeId"])
    private val sourceId: Long = checkNotNull(savedStateHandle["sourceId"])

    val source = sourcesManager.getOrStub(sourceId)

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

    private val _migrateHelperState = MutableStateFlow(MigrateHelperState())
    val migrateHelperState = _migrateHelperState.asStateFlow()

    init {
        val migrateIdString : String? = savedStateHandle["migrationId"]
        val migrateId : Long? = migrateIdString?.toLongOrNull()
        if(migrateId != null){
            viewModelScope.launch {
                val anime = getAnime.await(migrateId) ?: return@launch
                _migrateHelperState.update {
                    it.copy(
                        oldAnime = anime
                    )
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            combine(
                animeWithEpisodesInteractor.subscribe(animeId),
                dataStore.getPreferencesGroupAsFlow(BasePreferences).map { it.downloadedOnly }.distinctUntilChanged(),
            ) { animeWithEpisodes, downloadedOnly ->
                animeWithEpisodes to downloadedOnly
            }.collectLatest { (animeWithEpisodes, downloadedOnly) ->
                val (anime, episodes) = animeWithEpisodes
                _uiState.update { currentState ->
                    currentState.copy(
                        details = anime,
                        downloadedOnly = downloadedOnly,
                        episodes = episodes.sortedBy {
                            it.sourceOrder
                        }.toEpisodeItems(anime, downloadedOnly)
                    )
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            merge(
                downloadManager.queueState.map { },
                downloadManager.statusFlow().map { },
                downloadManager.cacheChanges.map { },
            ).collect {
                _uiState.update { currentState ->
                    val anime = currentState.details ?: return@update currentState
                    currentState.copy(
                        episodes = currentState.episodes
                            .withDownloadStates(anime)
                            // A deleted episode leaves the downloaded only list
                            .keepDisplayedSelected(anime.unseenFilterRaw, currentState.downloadedOnly)
                    )
                }
            }
        }
        viewModelScope.launch {
            downloadManager.progressFlow()
                .filter { it.anime.id == animeId }
                .collect { download ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            episodes = currentState.episodes.map {
                                if (it.episode.id != download.episode.id) return@map it
                                it.copy(download = it.download.copy(progress = download.progress))
                            }
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

            if (episodes.isEmpty()) {
                _episodesRefreshing.update { true }
                fetchEpisodesFromSource(anime)
            }
        }
    }

    // Update anime details functions

    private suspend fun fetchAnimeDetailsFromSource(anime: Anime) {
        val networkDetails = awaitOrNull(onError = { _detailsRefreshing.update { false } }) {
            source.callGuarded { getAnimeDetails(anime.toSAnime()) }
        }
        networkDetails?.let {
            updateAnimeInteractor.awaitUpdateFromSource(anime, it)
            _detailsRefreshing.update { false }
        }
    }

    private suspend fun fetchEpisodesFromSource(anime: Anime, manualFetch : Boolean = false) {
        val networkEpisodes = awaitOrNull(onError = { _episodesRefreshing.update { false } }) {
            source.callGuarded { getEpisodeList(anime.toSAnime()) }
        }
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
            if (anime.favorite) {
                updateAnimeInteractor.updateLibraryAnime(anime, false)
                return@launch
            }
            val defaultCategory = dataStore.getPreferencesGroup(LibraryPreferences).defaultCategory
            when (val choice = defaultCategoryChoice(defaultCategory, getCategories.await())) {
                is DefaultCategoryChoice.Ask -> showSetCategoriesDialog(addToLibrary = true)
                // Its categories first, so it joins the library in its tabs
                is DefaultCategoryChoice.Categories -> {
                    setAnimeCategories.await(anime.id, choice.ids)
                    updateAnimeInteractor.updateLibraryAnime(anime, true)
                }
            }
        }
    }

    // Categories

    fun showSetCategoriesDialog(addToLibrary: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val animeCategories = getCategories.await(animeId).map { it.id }
            val selection = categoryCheckboxes(getCategories.await(), listOf(animeCategories))
            _uiState.update { it.copy(setCategoriesDialog = SetCategoriesDialogState(selection, addToLibrary)) }
        }
    }

    fun dismissSetCategoriesDialog() {
        _uiState.update { it.copy(setCategoriesDialog = null) }
    }

    fun setCategories(add: List<Long>, remove: List<Long>) {
        val addToLibrary = uiState.value.setCategoriesDialog?.addToLibrary == true
        viewModelScope.launch(Dispatchers.IO) {
            setAnimeCategories.await(listOf(animeId), add, remove)
            if (addToLibrary) {
                val anime = animeWithEpisodesInteractor.awaitAnime(animeId)
                if (!anime.favorite) updateAnimeInteractor.updateLibraryAnime(anime, true)
            }
        }
    }

    fun setEpisodeFlags(flags : Long){
        viewModelScope.launch(Dispatchers.IO) {
            val anime = animeWithEpisodesInteractor.awaitAnime(animeId)
            updateAnimeInteractor.updateAnimeEpisodeFlags(anime,flags)
        }
    }

    // Downloads

    fun onDownloadAction(episodeItem: EpisodeItem, action: EpisodeDownloadAction) {
        val anime = uiState.value.details ?: return
        val episode = episodeItem.episode
        val download = downloadManager.queueState.value.find { it.episode.id == episode.id }
        when (action) {
            EpisodeDownloadAction.START -> downloadManager.downloadEpisodes(anime, listOf(episode))
            EpisodeDownloadAction.CANCEL -> download?.let { downloadManager.cancelQueuedDownloads(listOf(it)) }
            EpisodeDownloadAction.DELETE -> downloadManager.deleteEpisodes(anime, listOf(episode))
            EpisodeDownloadAction.RETRY -> download?.let { downloadManager.retryDownloads(listOf(it)) }
        }
    }

    fun runDownloadAction(action: DetailsDownloadAction) {
        val anime = uiState.value.details ?: return
        downloadManager.downloadEpisodes(anime, uiState.value.episodes.episodesToDownload(action, anime.unseenFilterRaw))
    }

    /** Deletes every download of the anime, after the user confirmed. */
    fun deleteAllDownloads() {
        val anime = uiState.value.details ?: return
        downloadManager.deleteAnime(anime)
    }

    fun downloadSelected() {
        val anime = uiState.value.details ?: return
        // In watch order: the list shows the newest first
        val episodes = uiState.value.episodes.filter { it.selected }.asReversed().map { it.episode }
        downloadManager.downloadEpisodes(anime, episodes)
        toggleAllSelectedEpisodes(false)
    }

    fun deleteSelectedDownloads() {
        val anime = uiState.value.details ?: return
        downloadManager.deleteEpisodes(anime, uiState.value.episodes.filter { it.selected }.map { it.episode })
        toggleAllSelectedEpisodes(false)
    }

    private fun List<EpisodeItem>.withDownloadStates(anime: Anime): List<EpisodeItem> {
        val states = downloadManager.getDownloadStates(anime, map { it.episode })
        return map { item -> states[item.episode.id]?.let { item.copy(download = it) } ?: item }
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
        updateDisplayedSelection { !it.selected }
    }

    fun toggleAllSelectedEpisodes(selected: Boolean) {
        updateDisplayedSelection { selected }
    }

    private fun updateDisplayedSelection(selected: (EpisodeItem) -> Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                episodes = currentState.episodes
                    .selectDisplayed(currentState.details?.unseenFilterRaw, currentState.downloadedOnly, selected)
                    .also { syncSelectedEpisodesIds(it) }
            )
        }
    }

    private fun syncSelectedEpisodesIds(episodes: List<EpisodeItem>) {
        selectedEpisodesIds.clear()
        episodes.filter { it.selected }.mapTo(selectedEpisodesIds) { it.episode.id }
    }

    private fun List<Episode>.toEpisodeItems(anime: Anime, downloadedOnly: Boolean): List<EpisodeItem> {
        return this.map {
            EpisodeItem(
                it,
                it.id in selectedEpisodesIds
            )
        }
            // Before the filters: the downloaded only mode filters on the download states
            .withDownloadStates(anime)
            .keepDisplayedSelected(anime.unseenFilterRaw, downloadedOnly)
    }

    // An episode the filters now hide doesn't stay selected
    private fun List<EpisodeItem>.keepDisplayedSelected(unseenFilter: Long?, downloadedOnly: Boolean): List<EpisodeItem> {
        return selectDisplayed(unseenFilter, downloadedOnly) { it.selected }
            .also { syncSelectedEpisodesIds(it) }
    }
}

// Select all and invert act on the displayed episodes only, the hidden ones end up unselected
internal fun List<EpisodeItem>.selectDisplayed(
    unseenFilter: Long?,
    downloadedOnly: Boolean,
    selected: (EpisodeItem) -> Boolean
): List<EpisodeItem> {
    return map { it.copy(selected = it.isDisplayed(unseenFilter, downloadedOnly) && selected(it)) }
}