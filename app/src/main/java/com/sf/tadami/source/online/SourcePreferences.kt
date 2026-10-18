package com.sf.tadami.source.online

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.sf.tadami.DataStoresHandler
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
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

/**
 * The preferences of the source [sourceId], read from its data store and kept up to date, for
 * [ConfigurableParsedHttpAnimeSource] and [ConfigurableAnimeHttpSource].
 */
internal class SourcePreferences<T : CustomPreferencesIdentifier>(
    sourceId: Long,
    fileName: String,
    prefGroup: CustomPreferences<T>,
) {
    val dataStore: DataStore<Preferences> = Injekt.get<DataStoresHandler>().getDataStore(sourceId, fileName)

    // Loaded synchronously so the preferences are already up to date in the sources' init
    private val _state: MutableStateFlow<T> = MutableStateFlow(
        try {
            runBlocking { dataStore.getPreferencesGroup(prefGroup) }
        } catch (e: Exception) {
            prefGroup.transform(emptyPreferences())
        }
    )
    val state: StateFlow<T> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            dataStore.getPreferencesGroupAsFlow(prefGroup).collectLatest {
                _state.value = it
            }
        }
    }

    /** Stops following the data store, see [ConfigurableParsedHttpAnimeSource.cancelPreferencesUpdates]. */
    fun cancelUpdates() {
        scope.cancel()
    }

    companion object {
        /** The name of the data store file of the source [sourceId]. */
        fun fileName(sourceId: Long) = "anime_source_$sourceId"
    }
}
