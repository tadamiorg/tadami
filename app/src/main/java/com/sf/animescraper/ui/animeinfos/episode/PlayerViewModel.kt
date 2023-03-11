package com.sf.animescraper.ui.animeinfos.episode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.animescraper.data.episode.EpisodeRepository
import com.sf.animescraper.data.interactors.AnimeWithEpisodesInteractor
import com.sf.animescraper.domain.episode.Episode
import com.sf.animescraper.network.api.model.StreamSource
import com.sf.animescraper.network.api.online.AnimeSource
import com.sf.animescraper.network.requests.utils.ObserverAS
import com.sf.animescraper.ui.tabs.animesources.AnimeSourcesManager
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class PlayerViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val episodeRepository: EpisodeRepository = Injekt.get()
    private val animeWithEpisodesInteractor: AnimeWithEpisodesInteractor = Injekt.get()
    private val sourcesManager: AnimeSourcesManager = Injekt.get()

    private val initialEpisode: Long = checkNotNull(savedStateHandle["episode"])
    private val sourceId: String = checkNotNull(savedStateHandle["sourceId"])

    private val source: AnimeSource = checkNotNull(sourcesManager.getExtensionById(sourceId))

    private val _currentEpisode: MutableStateFlow<Episode?> = MutableStateFlow(null)
    val currentEpisode: StateFlow<Episode?> = _currentEpisode.asStateFlow()

    private val updatedEpisode = currentEpisode.shareIn(viewModelScope, SharingStarted.Eagerly)

    private val _episodesList: MutableStateFlow<List<Episode>> = MutableStateFlow(emptyList())
    val episodes = _episodesList.asStateFlow()

    private val _animeTitle: MutableStateFlow<String> = MutableStateFlow("")
    val animeTitle = _animeTitle.asStateFlow()

    private val _isFetchingSources: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFetchingSources = _isFetchingSources.asStateFlow()

    private val _isNewEpisode: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isNewEpisode = _isNewEpisode.asStateFlow()

    val playerScreenLoading = combine(animeTitle, episodes) { title, episodes ->
        title.isEmpty() || episodes.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var fetchEpisodeDisposable: Disposable? = null

    private val _uiState = MutableStateFlow(EpisodeUiState())
    val uiState: StateFlow<EpisodeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            updatedEpisode.collectLatest {
                it?.let {
                    selectEpisode(it.url)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val selectedEpisode = episodeRepository.getEpisodeById(initialEpisode)

            animeWithEpisodesInteractor.awaitBoth(selectedEpisode.animeId)
                .let { (anime, episodes) ->
                    _animeTitle.update { anime.title }
                    _episodesList.update { episodes }
                    _currentEpisode.update { selectedEpisode }
                }
        }
    }

    fun setCurrentEpisode(episode: Episode) {
        _currentEpisode.update { episode }
    }

    fun selectSource(source: StreamSource) {
        _uiState.update { currentState ->
            currentState.copy(selectedSource = source)
        }
    }

    fun newEpisodeLoaded() {
        _isNewEpisode.update { false }
    }

    private fun selectEpisode(
        url: String
    ) {
        _isFetchingSources.update { true }
        _isNewEpisode.update { true }
        fetchEpisodeDisposable?.dispose()

        source.fetchEpisode(url).subscribeOn(Schedulers.io())
            .subscribe(
                object : ObserverAS<List<StreamSource>>() {
                    override fun onNext(data: List<StreamSource>) {
                        _uiState.update { currentState ->
                            currentState.copy(
                                rawUrl = url,
                                selectedSource = data.firstOrNull(),
                                availableSources = data
                            )
                        }
                        _isFetchingSources.update { false }
                        super.onNext(data)
                    }

                    override fun onSubscribe(d: Disposable) {
                        super.onSubscribe(d)
                        fetchEpisodeDisposable = d
                    }

                    override fun onError(e: Throwable) {
                        _isFetchingSources.update { false }
                        super.onError(e)
                    }
                }
            )
    }
}