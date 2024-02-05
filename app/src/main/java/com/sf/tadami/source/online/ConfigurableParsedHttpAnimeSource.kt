package com.sf.tadami.source.online

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.navigation.NavHostController
import com.sf.tadami.ScopesHandler
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import com.sf.tadami.ui.tabs.settings.components.PreferenceScreen
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

abstract class ConfigurableParsedHttpAnimeSource<T : CustomPreferencesIdentifier>(prefGroup: CustomPreferences<T>) :
    ParsedAnimeHttpSource() {

    // Preferences
    private val PREFERENCES_FILE_NAME : String by lazy { "anime_source_$id" }

    private val scopesHandler : ScopesHandler = Injekt.get()

    val dataStore: DataStore<Preferences> by lazy {
        val newScope = CoroutineScope(Dispatchers.IO+ SupervisorJob())
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

    val preferences : T by lazy {
        runBlocking {
            dataStore.getPreferencesGroup(prefGroup)
        }
    }

    abstract fun getPreferenceScreen(navController: NavHostController): PreferenceScreen
}