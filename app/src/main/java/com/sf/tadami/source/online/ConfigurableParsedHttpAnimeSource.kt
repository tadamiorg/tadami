package com.sf.tadami.source.online

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.sf.tadami.ScopesHandler
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import com.sf.tadami.ui.tabs.browse.tabs.sources.preferences.SourcesPreferencesContent
import com.sf.tadami.utils.CollectAsStateValue
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    private val scopesHandler: ScopesHandler = Injekt.get()

    private val _preferences: MutableStateFlow<T> = MutableStateFlow(
        prefGroup.transform(
            emptyPreferences()
        )
    )
    private val preferencesState: StateFlow<T> = _preferences.asStateFlow()
    val preferences by CollectAsStateValue(preferencesState)


    val dataStore: DataStore<Preferences> by lazy {
        val newScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scopesHandler.dataStoreScopes[id] = newScope
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            migrations = listOf(
                SharedPreferencesMigration(
                    Injekt.get<Application>(),
                    PREFERENCES_FILE_NAME
                )
            ),
            scope = newScope,
            produceFile = {
                Injekt.get<Application>().preferencesDataStoreFile(PREFERENCES_FILE_NAME)
            }
        )
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

