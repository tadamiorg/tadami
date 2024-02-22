package com.sf.tadami.ui.tabs.browse.tabs.sources.preferences

import com.sf.tadami.preferences.model.SourcePreference

data class SourcePreferencesUiState(
    val tabTitle : String,
    val preferencesItems : List<SourcePreference> = emptyList()
)