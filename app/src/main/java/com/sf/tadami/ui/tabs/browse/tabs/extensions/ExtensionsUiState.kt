package com.sf.tadami.ui.tabs.browse.tabs.extensions

import androidx.compose.runtime.Immutable
import com.sf.tadami.extension.model.LoadResult

typealias ItemGroups = MutableMap<ExtensionUiModel.Header, List<ExtensionUiModel.Item>>

@Immutable
data class ExtensionsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: ItemGroups = mutableMapOf(),
    val failedExtensions: List<LoadResult.Error> = emptyList(),
    val updates: Int = 0,
    /** Installed extension packages left over from before extensions moved into private storage. */
    val redundantPackages: List<String> = emptyList(),
    val searchQuery: String = "",
) {
    val isEmpty = items.isEmpty() && failedExtensions.isEmpty()
}