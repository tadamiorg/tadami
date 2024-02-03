package com.sf.tadami.ui.animeinfos.episode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sf.tadami.R
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.data.interactors.anime.AnimeWithEpisodesInteractor
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.data.interactors.history.UpdateHistoryInteractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.network.utils.TadaErrorConsumer
import com.sf.tadami.source.model.StreamSource
import com.sf.tadami.source.online.StubSource
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.utils.SaveableMutableSaveStateFlow
import com.sf.tadami.ui.utils.UiToasts
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get


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

    private val sourceId: Long = checkNotNull(savedStateHandle["sourceId"])

    private val _episodeId = SaveableMutableSaveStateFlow<Long>(
        savedStateHandle, "episode", checkNotNull(savedStateHandle["episode"])
    )
    private val episodeId = _episodeId.asStateFlow()

    private val source = sourcesManager.getOrStub(sourceId)

    private val _currentEpisode: MutableStateFlow<Episode?> = MutableStateFlow(null)
    val currentEpisode: StateFlow<Episode?> = _currentEpisode.asStateFlow()

    private val updatedEpisode = currentEpisode.shareIn(viewModelScope, SharingStarted.Eagerly)

    private val _episodesList: MutableStateFlow<List<Episode>> = MutableStateFlow(emptyList())
    val episodes = _episodesList.asStateFlow()

    val hasNextIterator = combine(currentEpisode, episodes) { ep, epList ->
        when {
            ep != null && epList.isNotEmpty() -> epList.listIterator(epList.indexOfFirst { it.id == ep.id })
            else -> {
                epList.listIterator()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _episodesList.value.listIterator())

    val hasPreviousIterator = combine(currentEpisode, episodes) { ep, epList ->
        when {
            ep != null && epList.isNotEmpty() -> epList.listIterator(epList.indexOfFirst { it.id == ep.id } + 1)
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

    private val _uiState = MutableStateFlow(EpisodeUiState())
    val uiState: StateFlow<EpisodeUiState> = _uiState.asStateFlow()

    private val _idleLock = MutableStateFlow(false)
    val idleLock = _idleLock.asStateFlow()

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
    }

    fun getDbEpisodeTime(callback: (time: Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val selectedEpisode =
                currentEpisode.value?.let { episodeRepository.getEpisodeById(it.id) }
            withContext(Dispatchers.Main) {
                callback(selectedEpisode?.timeSeen ?: 0)
            }
        }
    }

    fun setCurrentEpisode(episode: Episode) {
        _currentEpisode.update { episode }
    }

    fun selectSource(source: StreamSource?) {
        setIdleLock(true)
        _uiState.update { currentState ->
            currentState.copy(selectedSource = source)
        }
    }

    fun updateTime(episode: Episode?, totalTime: Long, timeSeen: Long, threshold: Int): Job? {
        episode?.let { ep ->
            if (ep.seen) return null
            return viewModelScope.launch(Dispatchers.IO) {
                if (totalTime > 0L && timeSeen > 999L) {
                    val watched = (timeSeen.toDouble() / totalTime) * 100 > threshold
                    if (watched) {
                        updateAnimeInteractor.awaitSeenEpisodeUpdate(setOf(ep.id), true)
                    } else {
                        updateAnimeInteractor.awaitSeenEpisodeTimeUpdate(ep, totalTime, timeSeen)
                    }
                }
            }
        }
        return null
    }

    fun setResumeFromCastSession(
        rawUrl: String,
        selectedSource: StreamSource,
        availableSources: List<StreamSource>
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                rawUrl = rawUrl,
                selectedSource = selectedSource,
                availableSources = availableSources
            )
        }
    }

    fun setIdleLock(locked: Boolean) {
        _idleLock.update { locked }
    }

    private fun selectEpisode(
        episode: Episode
    ) {
        setIdleLock(true)
        _uiState.updateAndGet { currentState ->
            currentState.copy(
                rawUrl = null,
                selectedSource = null,
                availableSources = emptyList()
            )
        }
        _isFetchingSources.update { true }
        fetchEpisodeDisposable?.dispose()

        fetchEpisodeDisposable = source.fetchEpisode(episode.url).subscribe(
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
                if(error is StubSource.SourceNotInstalledException){
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
}