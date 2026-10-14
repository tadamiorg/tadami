package com.sf.tadami.ui.tabs.browse.tabs.extensions

import androidx.compose.runtime.Immutable

typealias ItemGroups = MutableMap<ExtensionUiModel.Header, List<ExtensionUiModel.Item>>

@Immutable
data class ExtensionsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: ItemGroups = mutableMapOf(),
    val updates: Int = 0,
    /** The extensions installed as apps that are copied into the app's storage, so not needed. */
    val legacyPackages: List<String> = emptyList(),
    /** Whether the extensions weren't loaded at startup, after crashes. */
    val safeMode: Boolean = false,
    val searchQuery: String = "",
) {
    val isEmpty = items.isEmpty()
}