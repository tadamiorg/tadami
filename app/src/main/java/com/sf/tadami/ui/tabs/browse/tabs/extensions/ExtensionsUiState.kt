package com.sf.tadami.ui.tabs.browse.tabs.extensions

import androidx.compose.runtime.Immutable
import com.sf.tadami.preferences.extensions.ExtensionInstallerEnum

typealias ItemGroups = MutableMap<ExtensionUiModel.Header, List<ExtensionUiModel.Item>>

@Immutable
data class ExtensionsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: ItemGroups = mutableMapOf(),
    val updates: Int = 0,
    val installer: ExtensionInstallerEnum? = null,
    val searchQuery: String = "",
) {
    val isEmpty = items.isEmpty()
}