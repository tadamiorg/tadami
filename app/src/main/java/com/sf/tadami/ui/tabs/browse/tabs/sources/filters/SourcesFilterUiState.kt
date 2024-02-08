package com.sf.tadami.ui.tabs.browse.tabs.sources.filters

import androidx.compose.runtime.Immutable
import com.sf.tadami.domain.source.Source
import java.util.SortedMap

sealed interface SourcesFilterUiState {

    @Immutable
    data object Loading : SourcesFilterUiState

    @Immutable
    data class Error(
        val throwable: Throwable,
    ) : SourcesFilterUiState

    @Immutable
    data class Success(
        val items: SortedMap<String, List<Source>>,
        val enabledLanguages: Set<String>,
        val hiddenSources: Set<String>,
    ) : SourcesFilterUiState {

        val isEmpty: Boolean
            get() = items.isEmpty()
    }
}