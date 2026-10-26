package com.sf.tadami.source.online

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import com.sf.tadami.source.ConfigurableSource
import com.sf.tadami.ui.tabs.browse.tabs.sources.preferences.SourcesPreferencesContent
import com.sf.tadami.utils.CollectAsStateValue
import kotlinx.coroutines.flow.StateFlow

abstract class ConfigurableParsedHttpAnimeSource<T : CustomPreferencesIdentifier>(
    sourceId: Long,
    prefGroup: CustomPreferences<T>
) :
    ParsedAnimeHttpSource(sourceId), ConfigurableSource {

    // Preferences
    val PREFERENCES_FILE_NAME: String by lazy { SourcePreferences.fileName(id) }

    private val sourcePreferences = SourcePreferences(id, PREFERENCES_FILE_NAME, prefGroup)

    final override val dataStore: DataStore<Preferences>
        get() = sourcePreferences.dataStore

    val preferencesState: StateFlow<T> = sourcePreferences.state
    val preferences by CollectAsStateValue(preferencesState)

    /**
     * Stops listening to the preferences changes, called once this instance has been replaced
     * or removed so it doesn't stay referenced by the data store.
     */
    internal fun cancelPreferencesUpdates() {
        sourcePreferences.cancelUpdates()
    }

    abstract override fun getPreferenceScreen(): SourcesPreferencesContent
}
