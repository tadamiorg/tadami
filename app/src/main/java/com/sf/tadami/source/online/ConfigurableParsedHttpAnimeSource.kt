package com.sf.tadami.source.online

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.sf.tadami.DataStoresHandler
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import com.sf.tadami.ui.tabs.browse.tabs.sources.preferences.SourcesPreferencesContent
import com.sf.tadami.utils.CollectAsStateValue
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

abstract class ConfigurableParsedHttpAnimeSource<T : CustomPreferencesIdentifier>(
    sourceId: Long,
    prefGroup: CustomPreferences<T>
) :
    ParsedAnimeHttpSource(sourceId) {

    // Preferences
    val PREFERENCES_FILE_NAME: String by lazy { "anime_source_$id" }

    private val dataStoresHandler: DataStoresHandler = Injekt.get()

    // Declared before _preferences, which reads it during its initialisation
    val dataStore: DataStore<Preferences> by lazy {
        dataStoresHandler.getDataStore(id, PREFERENCES_FILE_NAME)
    }

    // Loaded synchronously so the preferences are already up to date in the subclasses' init
    private val _preferences: MutableStateFlow<T> = MutableStateFlow(
        try {
            runBlocking { dataStore.getPreferencesGroup(prefGroup) }
        } catch (e: Exception) {
            prefGroup.transform(emptyPreferences())
        }
    )
    val preferencesState: StateFlow<T> = _preferences.asStateFlow()
    val preferences by CollectAsStateValue(preferencesState)

    private val preferencesScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        preferencesScope.launch {
            dataStore.getPreferencesGroupAsFlow(prefGroup).collectLatest {
                _preferences.value = it
            }
        }
    }

    /**
     * Stops listening to the preferences changes, called once this instance has been replaced
     * or removed so it doesn't stay referenced by the data store.
     */
    internal fun cancelPreferencesUpdates() {
        preferencesScope.cancel()
    }

    abstract fun getPreferenceScreen(): SourcesPreferencesContent
}

