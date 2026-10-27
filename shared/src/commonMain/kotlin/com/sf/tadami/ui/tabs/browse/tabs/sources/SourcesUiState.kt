package com.sf.tadami.ui.tabs.browse.tabs.sources

import androidx.compose.runtime.Immutable

@Immutable
data class SourcesUiState(
    val isLoading: Boolean = true,
    val items: List<SourcesUiModel> = emptyList(),
) {
    val isEmpty = items.isEmpty()
}