package com.sf.tadami.ui.tabs.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.interactors.history.GetHistoryInteractor
import com.sf.tadami.data.interactors.history.GetNextEpisodeInteractor
import com.sf.tadami.data.interactors.history.RemoveHistoryInteractor
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.domain.history.HistoryWithRelations
import com.sf.tadami.ui.utils.insertSeparators
import com.sf.tadami.ui.utils.toDateKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel : ViewModel() {
    private val getHistoryInteractor: GetHistoryInteractor = Injekt.get()
    private val getNextEpisodeInteractor: GetNextEpisodeInteractor = Injekt.get()
    private val removeHistoryInteractor: RemoveHistoryInteractor = Injekt.get()

    private var _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            uiState.map {
                it.searchQuery
            }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    getHistoryInteractor.subscribe(query ?: "")
                        .distinctUntilChanged()
                        .catch { error ->
                            Log.e("HistoryError", error.stackTraceToString())
                        }
                        .map { it.toHistoryUiModels() }
                        .flowOn(Dispatchers.IO)
                }
                .collect { newList -> _uiState.update { it.copy(list = newList) } }
        }
    }

    private fun List<HistoryWithRelations>.toHistoryUiModels(): List<HistoryUiModel> {
        return map { HistoryUiModel.Item(it) }
            .insertSeparators { before, after ->
                val beforeDate = before?.item?.seenAt?.time?.toDateKey() ?: Date(0)
                val afterDate = after?.item?.seenAt?.time?.toDateKey() ?: Date(0)
                when {
                    beforeDate.time != afterDate.time && afterDate.time != 0L -> HistoryUiModel.Header(
                        afterDate
                    )
                    // Return null to avoid adding a separator between two items.
                    else -> null
                }
            }
    }

    fun updateSearchQuery(query: String?) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setHistoryToDelete(newHistory: HistoryWithRelations?) {
        _uiState.update { it.copy(historyToDelete = newHistory) }
    }

    fun removeFromHistory(history: HistoryWithRelations) {
        viewModelScope.launch(Dispatchers.IO) {
            removeHistoryInteractor.await(history)
        }
    }

    fun removeAllFromHistory(animeId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            removeHistoryInteractor.await(animeId)
        }
    }

    fun removeAllHistory(callBack : suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = removeHistoryInteractor.awaitAll()
            if(result){
                callBack()
            }
        }
    }

    fun getNextEpisodeForAnime(animeId: Long, episodeId: Long, callback: suspend CoroutineScope.(Episode?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val nextEpisodes = getNextEpisodeInteractor.await(animeId, episodeId,onlyUnseen = false)
            val nextEpisode = nextEpisodes.firstOrNull()
            withContext(Dispatchers.Main) {
                callback(nextEpisode)
            }
        }
    }
}