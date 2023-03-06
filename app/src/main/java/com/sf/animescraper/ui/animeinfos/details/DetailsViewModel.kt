package com.sf.animescraper.ui.animeinfos.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.animescraper.Database
import com.sf.animescraper.network.requests.okhttp.Callback
import com.sf.animescraper.network.requests.utils.ObserverAS
import com.sf.animescraper.network.scraping.AnimeSource
import com.sf.animescraper.network.scraping.dto.details.AnimeDetails
import com.sf.animescraper.network.scraping.dto.details.DetailsEpisode
import com.sf.animescraper.network.scraping.dto.search.Anime
import com.sf.animescraper.ui.shared.SharedViewModel
import data.Episode
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DetailsViewModel(
    sharedViewModel: SharedViewModel = Injekt.get()
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private var _detailsRefreshing = MutableStateFlow(false)
    private val detailsRefreshing = _detailsRefreshing.asStateFlow()

    private var _episodesRefreshing = MutableStateFlow(false)
    private val episodesRefreshing = _episodesRefreshing.asStateFlow()

    val source = sharedViewModel.uiState.value.source as AnimeSource
    val anime = sharedViewModel.uiState.value.selectedAnime as Anime

    init {
        val details = AnimeDetails.create()

        details.apply {
            url = anime.url
            title = anime.title
            thumbnail_url = anime.image
        }
        setDetails(details)
        refresh()
    }

    val isRefreshing: StateFlow<Boolean> =
        combine(episodesRefreshing, detailsRefreshing) { values ->
            values.any { it }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)


    private fun getDetails(callback: Callback<AnimeDetails>? = null) {
        _detailsRefreshing.update { true }
        viewModelScope.launch(Dispatchers.IO) {
            source.fetchAnimeDetails(anime).subscribe(object : ObserverAS<AnimeDetails>(callback) {
                override fun onNext(data: AnimeDetails) {
                    setDetails(data)
                    _detailsRefreshing.update { false }
                    super.onNext(data)
                }

                override fun onError(e: Throwable) {
                    _detailsRefreshing.update { false }
                    super.onError(e)
                }
            })
        }
    }

    private fun getEpisodes(callback: Callback<List<DetailsEpisode>>? = null) {
        _episodesRefreshing.update { true }
        viewModelScope.launch(Dispatchers.IO) {

            source.fetchEpisodesList(anime).subscribeOn(Schedulers.io())
                .subscribe(object : ObserverAS<List<DetailsEpisode>>(callback) {
                    override fun onNext(data: List<DetailsEpisode>) {
                        setEpisodes(data)
                        _episodesRefreshing.update { false }
                        super.onNext(data)
                    }

                    override fun onError(e: Throwable) {
                        _episodesRefreshing.update { false }
                        super.onError(e)
                    }
                })
        }
    }

    fun setDetails(details: AnimeDetails) {
        _uiState.update { currentState ->
            currentState.copy(details = details)
        }
    }

    private fun setEpisodes(episodes: List<DetailsEpisode>) {
        _uiState.update { currentState ->
            currentState.copy(episodes = episodes)
        }
    }

    fun refresh() {
        getDetails()
        getEpisodes()
    }
}