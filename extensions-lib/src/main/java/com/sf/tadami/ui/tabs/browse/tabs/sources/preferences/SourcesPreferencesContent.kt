package com.sf.tadami.ui.tabs.browse.tabs.sources.preferences

import com.sf.tadami.preferences.model.SourcePreference

data class SourcesPreferencesContent(
    val title : String,
    val preferences : List<SourcePreference>
)

