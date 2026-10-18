package com.sf.tadami.ui.tabs.browse.tabs.sources.preferences

import com.sf.tadami.preferences.model.SourcePreference

/**
 * The settings screen of a source, see [com.sf.tadami.source.ConfigurableSource.getPreferenceScreen].
 *
 * @property title the title of the screen
 * @property preferences its entries, usually [SourcePreference.PreferenceCategory]
 * @since extensions-lib 1.5
 */
data class SourcesPreferencesContent(
    val title : String,
    val preferences : List<SourcePreference>
)
