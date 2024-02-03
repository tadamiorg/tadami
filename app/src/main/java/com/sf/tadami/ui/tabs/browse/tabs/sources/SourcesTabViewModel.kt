package com.sf.tadami.ui.tabs.browse.tabs.sources

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sf.tadami.data.interactors.sources.GetEnabledSources
import com.sf.tadami.domain.source.Source
import com.sf.tadami.utils.Lang
import com.sf.tadami.utils.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import okhttp3.internal.toImmutableList
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.TreeMap

class SourcesTabViewModel : ViewModel() {
    private val getEnabledSources: GetEnabledSources = Injekt.get()

    private val _uiState = MutableStateFlow(SourcesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launchIO {
            getEnabledSources.subscribe()
                .catch {
                    Log.e("SourcesTabViewModelInit", it.stackTraceToString())
                }
                .collectLatest(::collectLatestSources)
        }
    }

    private fun collectLatestSources(sources: List<Source>) {
        _uiState.update { state ->
            val map = TreeMap<Lang, MutableList<Source>> { d1, d2 ->
                // Sources without a lang defined will be placed at the end
                when {
                    d1 == Lang.UNKNOWN && d2 != Lang.UNKNOWN -> 1
                    d2 == Lang.UNKNOWN && d1 != Lang.UNKNOWN -> -1
                    else -> d1.compareTo(d2)
                }
            }
            val byLang = sources.groupByTo(map) {
                when {
                    else -> it.lang
                }
            }

            state.copy(
                isLoading = false,
                items = byLang
                    .flatMap {
                        listOf(
                            SourcesUiModel.Header(it.key.getRes()),
                            *it.value.map { source ->
                                SourcesUiModel.Item(source)
                            }.toTypedArray(),
                        )
                    }
                    .toImmutableList(),
            )
        }
    }
}