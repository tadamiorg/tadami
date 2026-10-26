package com.sf.tadami.source

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.ui.tabs.browse.tabs.sources.preferences.SourcesPreferencesContent

/**
 * A source with preferences the user edits in its settings screen.
 *
 * @since extensions-lib 1.7, implemented by ConfigurableParsedHttpAnimeSource and
 * ConfigurableAnimeHttpSource
 */
interface ConfigurableSource : Source {

    /** Where its preferences are stored, the file `anime_source_<id>`. */
    val dataStore: DataStore<Preferences>

    /** The settings screen of the source: its title and its preferences. */
    fun getPreferenceScreen(): SourcesPreferencesContent
}
