package com.sf.tadami.ui.tabs.browse.tabs.extensions.filters

import androidx.compose.runtime.Immutable
import com.sf.tadami.utils.Lang

sealed interface ExtensionsFilterUiState {

    @Immutable
    data object Loading : ExtensionsFilterUiState

    @Immutable
    data class Success(
        val languages: List<Lang>,
        val enabledLanguages: Set<String> = emptySet(),
    ) : ExtensionsFilterUiState {

        val isEmpty: Boolean
            get() = languages.isEmpty()
    }
}