package com.sf.tadami.source.online

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import com.sf.tadami.ui.tabs.browse.tabs.sources.preferences.SourcesPreferencesContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class ConfigurableParsedHttpAnimeSource<T : CustomPreferencesIdentifier>(sourceId : Long,prefGroup: CustomPreferences<T>) :
    ParsedAnimeHttpSource(sourceId) {

    // Preferences
    private val PREFERENCES_FILE_NAME : String = throw Exception("Stub!")

    val dataStore: DataStore<Preferences> = throw Exception("Stub!")

    private val _preferences: MutableStateFlow<T> = throw Exception("Stub!")
    val preferencesState: StateFlow<T> = throw Exception("Stub!")
    val preferences : T = throw Exception("Stub!")

    abstract fun getPreferenceScreen(): SourcesPreferencesContent
}