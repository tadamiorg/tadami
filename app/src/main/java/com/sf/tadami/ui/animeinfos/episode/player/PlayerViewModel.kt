package com.sf.tadami.ui.animeinfos.episode.player

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sf.tadami.R
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.data.download.isLocalFile
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.data.interactors.anime.AnimeWithEpisodesInteractor
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.data.interactors.history.UpdateHistoryInteractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.domain.episode.isEpisodeWatched
import com.sf.tadami.extension.util.ExtensionDisabledException
import com.sf.tadami.extension.util.fetchGuarded
import com.sf.tadami.network.utils.TadaErrorConsumer
import com.sf.tadami.preferences.CommonKeys
import com.sf.tadami.preferences.download.DownloadPreferences
import com.sf.tadami.source.StubSource
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.model.Track
import com.sf.tadami.source.online.ConfigurableParsedHttpAnimeSource
import com.sf.tadami.ui.animeinfos.episode.EpisodeUiState
import com.sf.tadami.ui.animeinfos.episode.cast.CastRemoteState
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.utils.SaveableMutableSaveStateFlow
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.utils.getPreferencesGroup
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Suppress("UNCHECKED_CAST")
class PlayerViewModelFactory(
    private var isResumedFromCast: Boolean = false
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return PlayerViewModel(extras.createSavedStateHandle(), isResumedFromCast) as T
    }
}

class PlayerViewModel(
    savedStateHandle: SavedStateHandle,
    private var isResumedFromCast: Boolean = false
) : ViewModel() {

    private val episodeRepository: EpisodeRepository = Injekt.get()
    private val animeWithEpisodesInteractor: AnimeWithEpisodesInteractor = Injekt.get()
    private val updateAnimeInteractor: UpdateAnimeInteractor = Injekt.get()
    private val updateHistoryInteractor: UpdateHistoryInteractor = Injekt.get()
    private val sourcesManager: SourceManager = Injekt.get()
    private val downloadManager: TadamiDownloadManager = Injekt.get()
    private val dataStore: DataStore<Preferences> = Injekt.get()

    private val sourceId: Long = checkNotNull(savedStateHandle["sourceId"])

    private val _episodeId = SaveableMutableSaveStateFlow<Long>(
        savedStateHandle, "episode", checkNotNull(savedStateHandle["episode"])
    )
    private val episodeId = _episodeId.asStateFlow()

    val source = sourcesManager.getOrStub(sourceId)

    val sourceDataStore = if(source is ConfigurableParsedHttpAnimeSource<*>) {
        source.dataStore
    } else null

    val sourceDataStoreScreen = if(source is ConfigurableParsedHttpAnimeSource<*>) {
        source.getPreferenceScreen().preferences
    } else emptyList()

    // Handle tooltip infos to be shown during playback

    val sourceTooltipAuto = if(source is ConfigurableParsedHttpAnimeSource<*> && sourceDataStore != null ) {
        runBlocking {
            source.dataStore.data.first()[CommonKeys.SHOULD_SHOW_EPISODE_TOOLTIP] ?: false
        }
    } else null

    val sourceSupportTooltip = if(source is ConfigurableParsedHttpAnimeSource<*>) {
        source.supportEpisodeTooltip
    } else null

    val sourceTooltipContent = if(source is ConfigurableParsedHttpAnimeSource<*> && sourceSupportTooltip == true) {
        source.getEpisodeTooltip()
    } else null

    private val _currentEpisode: MutableStateFlow<Episode?> = MutableStateFlow(null)
    val currentEpisode: StateFlow<Episode?> = _currentEpisode.asStateFlow()

    private val updatedEpisode = currentEpisode.shareIn(viewModelScope, SharingStarted.Eagerly)

    private val _episodesList: MutableStateFlow<List<Episode>> = MutableStateFlow(emptyList())
    val episodes = _episodesList.asStateFlow()

    val hasNextIterator = combine(currentEpisode, episodes) { ep, epList ->
        val index = if (ep != null) epList.indexOfFirst { it.id == ep.id } else -1
        when {
            index >= 0 -> epList.listIterator(index)
            // Current episode missing from the list (e.g. re-inserted with a new id by a sync): no neighbours
            ep != null && epList.isNotEmpty() -> emptyList<Episode>().listIterator()
            else -> {
                epList.listIterator()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _episodesList.value.listIterator())

    val hasPreviousIterator = combine(currentEpisode, episodes) { ep, epList ->
        val index = if (ep != null) epList.indexOfFirst { it.id == ep.id } else -1
        when {
            index >= 0 -> epList.listIterator(index + 1)
            // Current episode missing from the list (e.g. re-inserted with a new id by a sync): no neighbours
            ep != null && epList.isNotEmpty() -> emptyList<Episode>().listIterator()
            else -> {
                epList.listIterator()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _episodesList.value.listIterator())

    private val _anime: MutableStateFlow<Anime?> = MutableStateFlow(null)
    val anime = _anime.asStateFlow()

    private val _isFetchingSources: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFetchingSources = _isFetchingSources.asStateFlow()

    val playerScreenLoading = combine(anime, episodes) { anime, episodes ->
        anime?.title?.isEmpty() ?: false || episodes.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var fetchEpisodeDisposable: Disposable? = null
    private var localEpisodeJob: Job? = null

    /** The downloaded files a cast replaced by the source's streams, and their episode id */
    @Volatile
    private var localBeforeCast: Pair<Long, StreamSource>? = null

    /** Downloaded episodes watched here, deleted once left when the settings say so */
    private val watchedDownloads = mutableSetOf<Episode>()

    private val _uiState = MutableStateFlow(EpisodeUiState())
    val uiState: StateFlow<EpisodeUiState> = _uiState.asStateFlow()

    private val _idleLock = MutableStateFlow(false)
    val idleLock = _idleLock.asStateFlow()

    private val _lockedControls = MutableStateFlow(false)
    val lockedControls = _lockedControls.asStateFlow()

    private val _playerInitiatedPause = MutableStateFlow(false)
    val playerInitiatedPause = _playerInitiatedPause.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            updatedEpisode.collectLatest {
                it?.let {
                    _episodeId.value = it.id
                    if (!isResumedFromCast) {
                        selectEpisode(it)
                    } else {
                        isResumedFromCast = false
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val selectedEpisode = episodeRepository.getEpisodeById(episodeId.value)

            _currentEpisode.update { selectedEpisode }

            animeWithEpisodesInteractor.subscribe(selectedEpisode.animeId)
                .collectLatest { (anime, episodes) ->
                    _anime.update { anime }
                    _episodesList.update { episodes.sortedBy { it.sourceOrder } }
                }
        }

        // Mirror TV-local track/source changes (received over the cast control channel) into the UI state so
        // the phone's cast dialogs reflect what the receiver is actually playing.
        viewModelScope.launch {
            CastRemoteState.selection.collect { sel ->
                sel ?: return@collect
                _uiState.update { st ->
                    val src = sel.sourceIndex?.let { st.availableSources.getOrNull(it) } ?: st.selectedSource
                    st.copy(
                        selectedSource = src,
                        selectedSubtitleTrack = when (sel.subtitleIndex) {
                            null -> st.selectedSubtitleTrack
                            -1 -> null
                            else -> src?.subtitleTracks?.getOrNull(sel.subtitleIndex)
                        },
                        selectedAudioTrack = sel.audioIndex?.let { src?.audioTracks?.getOrNull(it) }
                            ?: st.selectedAudioTrack,
                    )
                }
            }
        }
    }

    fun getDbEpisodeTime(callback: (time: Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedEpisode =
                currentEpisode.value?.let { episodeRepository.getEpisodeById(it.id) }
            withContext(Dispatchers.Main) {
                // A finished episode restarts from 0; otherwise resume at the saved time (0 if none).
                callback(if (selectedEpisode?.seen == true) 0L else selectedEpisode?.timeSeen ?: 0L)
            }
        }
    }

    fun setCurrentEpisode(episode: Episode) {
        _currentEpisode.update { episode }
    }

    fun selectSource(source: StreamSource?) {
        setIdleLock(true)
        _uiState.update { currentState ->
            // Reset the chosen subtitle so the new source auto-selects by language preference
            // (the old source's track object won't exist in the new source's list).
            currentState.copy(selectedSource = source, selectedSubtitleTrack = null)
        }
    }
    fun selectedSubtitleTrack(subtitleTrack: Track.SubtitleTrack?){
        _uiState.update { currentState ->
            currentState.copy(selectedSubtitleTrack = subtitleTrack)
        }
    }

    fun selectedAudioTrack(audioTrack: Track.AudioTrack?){
        _uiState.update { currentState ->
            currentState.copy(selectedAudioTrack = audioTrack)
        }
    }

    fun updateTime(episode: Episode?, totalTime: Long, timeSeen: Long, threshold: Int): Job? {
        episode?.let { ep ->
            if (ep.seen) return null
            return viewModelScope.launch(Dispatchers.IO) {
                if (totalTime > 0L && timeSeen > 999L) {
                    val watched = isEpisodeWatched(timeSeen, totalTime, threshold)
                    if (watched) {
                        updateAnimeInteractor.awaitSeenEpisodeUpdate(setOf(ep.id), true)
                        if (_uiState.value.selectedSource?.isLocalFile == true &&
                            dataStore.getPreferencesGroup(DownloadPreferences).deleteAfterWatched
                        ) {
                            synchronized(watchedDownloads) { watchedDownloads += ep }
                        }
                    } else {
                        updateAnimeInteractor.awaitSeenEpisodeTimeUpdate(ep, totalTime, timeSeen)
                    }
                }
            }
        }
        return null
    }

    fun getSourceHeaders() : Map<String,String>{
        return _uiState.value.selectedSource!!.headers!!.toMap()
    }

    fun setResumeFromCastSession(
        rawUrl: String,
        selectedSource: StreamSource,
        availableSources: List<StreamSource>,
        selectedTrack: Long? = null
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                rawUrl = rawUrl,
                selectedSource = selectedSource,
                availableSources = availableSources,
                selectedSubtitleTrack = selectedTrack?.toInt()
                    ?.let { selectedSource.subtitleTracks.getOrNull(it - 1) }
            )
        }
    }

    /**
     * The cast receiver switched episode on its own (from the TV or the notification): follow it
     * without fetching the sources or loading the cast again.
     */
    fun followCastEpisode(
        episode: Episode,
        rawUrl: String,
        selectedSource: StreamSource,
        availableSources: List<StreamSource>,
        selectedTrack: Long?
    ) {
        fetchEpisodeDisposable?.dispose()
        localEpisodeJob?.cancel()
        _isFetchingSources.update { false }
        // The episode first: the cast load guard compares it with the cast media when the sources change
        isResumedFromCast = true
        _currentEpisode.update { episode }
        setResumeFromCastSession(rawUrl, selectedSource, availableSources, selectedTrack)
    }

    fun setIdleLock(locked: Boolean) {
        _idleLock.update { locked }
    }

    fun lockControls(locked : Boolean){
        _lockedControls.update {
            locked
        }
    }

    fun setPlayerInitadtedPause(value : Boolean){
        _playerInitiatedPause.update {
            value
        }
    }

    private fun selectEpisode(
        episode: Episode
    ) {
        setIdleLock(true)
        _uiState.updateAndGet { currentState ->
            currentState.copy(
                rawUrl = null,
                selectedSource = null,
                selectedSubtitleTrack = null,
                selectedAudioTrack = null,
                availableSources = emptyList()
            )
        }
        _isFetchingSources.update { true }
        fetchEpisodeDisposable?.dispose()
        localEpisodeJob?.cancel()
        deleteWatchedDownloads(except = episode)

        // A downloaded episode plays from its files, without network
        localEpisodeJob = viewModelScope.launch(Dispatchers.IO) {
            val local = downloadManager.getLocalEpisode(episode)
                ?.toStreamSource(Injekt.get<Application>().getString(R.string.episode_downloaded))
            if (local == null) {
                fetchOnlineSources(episode)
                return@launch
            }
            _uiState.update { currentState ->
                currentState.copy(
                    rawUrl = episode.url,
                    selectedSource = local,
                    availableSources = listOf(local)
                )
            }
            _isFetchingSources.update { false }
            updateHistoryInteractor.awaitAnimeHistoryUpdate(episode)
        }
    }

    /**
     * The streams of the source instead of the downloaded files, for the current episode: the cast
     * receiver can't play the files the phone would serve (LocalCast).
     */
    fun fetchOnlineSources() {
        val episode = currentEpisode.value ?: return
        val local = _uiState.value.selectedSource?.takeIf { it.isLocalFile } ?: return
        localBeforeCast = episode.id to local
        _isFetchingSources.update { true }
        fetchOnlineSources(episode)
    }

    /**
     * The cast ended: the local player plays the downloaded files of the current episode again, when
     * still there, not the streams the cast needed. The player starts them at the saved time, where
     * the cast was.
     */
    fun restoreLocalSource() {
        val episode = currentEpisode.value ?: return
        val beforeCast = localBeforeCast
        localBeforeCast = null
        when (val next = CastFallback.afterCast(episode.id, _uiState.value.selectedSource, beforeCast)) {
            CastFallback.AfterCast.Keep -> Unit
            // No source while the files are checked: the player doesn't start the stream first
            is CastFallback.AfterCast.Restore -> {
                val online = _uiState.value.selectedSource
                localEpisodeJob?.cancel()
                _isFetchingSources.update { true }
                _uiState.update { it.copy(selectedSource = null) }
                localEpisodeJob = viewModelScope.launch(Dispatchers.IO) {
                    if (CastFallback.canRestore(next.source, downloadManager::localFileExists)) {
                        playLocal(next.source)
                    } else {
                        // Deleted during the cast: the stream goes on, from the saved time where the cast was
                        _isFetchingSources.update { false }
                        _uiState.update { it.copy(selectedSource = online) }
                    }
                }
            }
            CastFallback.AfterCast.Lookup -> {
                localEpisodeJob?.cancel()
                localEpisodeJob = viewModelScope.launch(Dispatchers.IO) {
                    val local = downloadManager.getLocalEpisode(episode)
                        ?.toStreamSource(Injekt.get<Application>().getString(R.string.episode_downloaded))
                    if (currentEpisode.value?.id != episode.id) return@launch
                    when {
                        local != null -> playLocal(local)
                        // Files the phone served for the cast, deleted since: the source's streams
                        CastFallback.needsOnlineStream(_uiState.value.selectedSource) -> {
                            _isFetchingSources.update { true }
                            fetchOnlineSources(episode)
                        }
                    }
                }
            }
        }
    }

    private fun playLocal(local: StreamSource) {
        fetchEpisodeDisposable?.dispose()
        _isFetchingSources.update { false }
        _uiState.update { currentState ->
            currentState.copy(
                selectedSource = local,
                availableSources = listOf(local),
                // The tracks chosen were the stream's: the files' are chosen by the preferences
                selectedSubtitleTrack = null,
                selectedAudioTrack = null,
            )
        }
    }

    private fun fetchOnlineSources(episode: Episode) {
        fetchEpisodeDisposable?.dispose()
        fetchEpisodeDisposable = source.fetchGuarded { fetchEpisodeSources(episode.url) }.subscribe(
            { data ->
                _uiState.updateAndGet { currentState ->
                    currentState.copy(
                        rawUrl = episode.url,
                        selectedSource = data.firstOrNull(),
                        availableSources = data
                    )
                }
                _isFetchingSources.update { false }
                if(data.isNotEmpty()){
                    viewModelScope.launch(Dispatchers.IO) {
                        updateHistoryInteractor.awaitAnimeHistoryUpdate(episode)
                    }
                }
            },
            TadaErrorConsumer {error, _, _ ->
                _isFetchingSources.update { false }
                // A downloaded episode still plays, only its cast failed
                val playsLocally = _uiState.value.selectedSource?.isLocalFile == true
                if(!playsLocally && (error is StubSource.SourceNotInstalledException || error is ExtensionDisabledException)){
                    _uiState.update {
                        it.copy(loadError = true)
                    }
                }
            },
            {
                if(_uiState.value.availableSources.isEmpty()){
                    UiToasts.showToast(R.string.player_screen_empty_sources)
                }
            }
        )
    }

    /** Not [except], the episode now played, even when watched before. */
    private fun deleteWatchedDownloads(except: Episode? = null) {
        val episodes = synchronized(watchedDownloads) {
            watchedDownloads.filter { it.id != except?.id }.also { watchedDownloads.clear() }
        }
        val anime = anime.value ?: return
        downloadManager.deleteEpisodes(anime, episodes)
    }

    override fun onCleared() {
        super.onCleared()
        fetchEpisodeDisposable?.dispose()
        deleteWatchedDownloads()
    }

}