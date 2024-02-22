package com.sf.tadami.source.online

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.sf.tadami.DataStoresHandler
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import com.sf.tadami.ui.tabs.browse.tabs.sources.preferences.SourcesPreferencesContent
import com.sf.tadami.utils.CollectAsStateValue
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

abstract class ConfigurableParsedHttpAnimeSource<T : CustomPreferencesIdentifier>(
    sourceId: Long,
    prefGroup: CustomPreferences<T>
) :
    ParsedAnimeHttpSource(sourceId) {

    // Preferences
    private val PREFERENCES_FILE_NAME: String by lazy { "anime_source_$id" }

    private val dataStoresHandler: DataStoresHandler = Injekt.get()

    private val _preferences: MutableStateFlow<T> = MutableStateFlow(
        prefGroup.transform(
            emptyPreferences()
        )
    )
    private val preferencesState: StateFlow<T> = _preferences.asStateFlow()
    val preferences by CollectAsStateValue(preferencesState)


    val dataStore: DataStore<Preferences> by lazy {
        dataStoresHandler.getDataStore(id, PREFERENCES_FILE_NAME)
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.getPreferencesGroupAsFlow(prefGroup).collectLatest {
                _preferences.value = it
            }
        }
    }

    abstract fun getPreferenceScreen(): SourcesPreferencesContent
}

