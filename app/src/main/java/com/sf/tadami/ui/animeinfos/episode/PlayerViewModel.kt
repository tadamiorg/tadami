package com.sf.tadami.ui.animeinfos.episode

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.data.interactors.AnimeWithEpisodesInteractor
import com.sf.tadami.data.interactors.UpdateAnimeInteractor
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.network.api.model.StreamSource
import com.sf.tadami.network.api.online.AnimeSource
import com.sf.tadami.network.requests.utils.TadaObserver
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesManager
import com.sf.tadami.ui.utils.SaveableMutableSaveStateFlow
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
    private val updateAnimeInteractor : UpdateAnimeInteractor = Injekt.get()
    private val sourcesManager: AnimeSourcesManager = Injekt.get()

    private val sourceId: String = checkNotNull(savedStateHandle["sourceId"])

    private val _episodeId = SaveableMutableSaveStateFlow<Long>(
        savedStateHandle, "episode", checkNotNull(savedStateHandle["episode"])
    )
    private val episodeId = _episodeId.asStateFlow()

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
                    _episodeId.value = it.id
                    selectEpisode(it)
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val selectedEpisode = episodeRepository.getEpisodeById(episodeId.value)

            _currentEpisode.update { selectedEpisode }

            animeWithEpisodesInteractor.subscribe(selectedEpisode.animeId).collectLatest {(anime, episodes) ->
                _animeTitle.update { anime.title }
                _episodesList.update { episodes }
            }
        }
    }

    fun setCurrentEpisode(episode: Episode) {
        _currentEpisode.update { episode }
    }

    fun selectSource(source: StreamSource?) {
        _uiState.update { currentState ->
            currentState.copy(selectedSource = source)
        }
    }

    fun updateTime(episode: Episode?,totalTime : Long,timeSeen : Long,threshold : Int){
        episode?.let { ep ->
            if(ep.seen) return
            viewModelScope.launch(Dispatchers.IO) {
                if (totalTime > 0L && timeSeen > 999L) {
                    val watched = (timeSeen.toDouble() / totalTime) * 100 > threshold
                    if(watched){
                        updateAnimeInteractor.awaitSeenEpisodeUpdate(setOf(ep.id),true)
                    }else{
                        updateAnimeInteractor.awaitSeenEpisodeTimeUpdate(ep, totalTime, timeSeen)
                    }
                }
            }
        }
    }

    private fun selectEpisode(
        episode: Episode
    ) {
        _uiState.updateAndGet { currentState ->
            currentState.copy(
                rawUrl = null,
                selectedSource = null,
                availableSources = emptyList()
            )
        }
        _isFetchingSources.update { true }
        fetchEpisodeDisposable?.dispose()

        source.fetchEpisode(episode.url).subscribeOn(Schedulers.io())
            .subscribe(
                object : TadaObserver<List<StreamSource>>() {
                    override fun onNext(data: List<StreamSource>) {
                        _uiState.updateAndGet { currentState ->
                            currentState.copy(
                                rawUrl = episode.url,
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