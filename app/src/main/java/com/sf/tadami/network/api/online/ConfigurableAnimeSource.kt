package com.sf.tadami.network.api.online

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.navigation.NavHostController
import com.sf.tadami.ui.tabs.settings.components.PreferenceScreen
import com.sf.tadami.ui.tabs.settings.model.CustomPreferences
import com.sf.tadami.ui.tabs.settings.model.CustomPreferencesIdentifier
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

abstract class ConfigurableParsedHttpAnimeSource<T : CustomPreferencesIdentifier> : ParsedAnimeHttpSource() {
    // Preferences

    val PREFERENCES_FILE_NAME = "anime_source_$id"

    protected abstract suspend fun getPrefGroup(): CustomPreferences<T>

    val dataStore: DataStore<Preferences> =  PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { emptyPreferences() }
        ),
        migrations = listOf(SharedPreferencesMigration(Injekt.get<Application>(), PREFERENCES_FILE_NAME)),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { Injekt.get<Application>().preferencesDataStoreFile(PREFERENCES_FILE_NAME) }
    )

    var preferences = runBlocking {
        dataStore.getPreferencesGroup(getPrefGroup())
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val initializedPrefGroup = getPrefGroup()
            dataStore.getPreferencesGroupAsFlow(initializedPrefGroup).collectLatest {
                preferences = it
            }
        }
    }

    abstract fun getPreferenceScreen(navController: NavHostController) : PreferenceScreen
}