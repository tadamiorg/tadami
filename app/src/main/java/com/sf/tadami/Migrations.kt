package com.sf.tadami

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.sf.tadami.notifications.backup.BackupCreateWorker
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateWorker
import com.sf.tadami.preferences.backup.BackupPreferences
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.source.online.ConfigurableParsedHttpAnimeSource
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.utils.Lang
import com.sf.tadami.utils.Lang.Companion.toPref
import com.sf.tadami.utils.clearAllPreferences
import com.sf.tadami.utils.clearPreferences
import com.sf.tadami.utils.createPreference
import com.sf.tadami.utils.editPreference
import com.sf.tadami.utils.getDataStoreValues
import java.io.File

object Migrations {

    /**
     * Performs a migration when the application is updated.
     *
     * @return true if a migration is performed, false otherwise.
     */
    suspend fun upgrade(
        context: Context,
        dataStore: DataStore<Preferences>,
        sourcesManager: SourceManager,
        appPreferences: AppPreferences,
        libraryPreferences: LibraryPreferences,
        playerPreferences: PlayerPreferences,
        backupPreferences: BackupPreferences,
        sourcesPreferences: SourcesPreferences
    ) {
        val oldVersion = appPreferences.lastVersionCode
        if (oldVersion < BuildConfig.VERSION_CODE) {
            dataStore.editPreference(
                BuildConfig.VERSION_CODE,
                intPreferencesKey(AppPreferences.LAST_VERSION_CODE.name)
            )

            // Always set up background tasks to ensure they're running
            LibraryUpdateWorker.setupTask(context)
            BackupCreateWorker.setupTask(context)

            // Fresh install
            if (oldVersion == 0) {
                return
            }

            if (oldVersion < 20) {
                replacePreferences(
                    dataStore = dataStore,
                    filterPredicate = {
                        sourcesPreferences.enabledLanguages.isNotEmpty() && it.key.name == SourcesPreferences.ENABLED_LANGUAGES.name
                    },
                    newKey = { it.name },
                    newValue = {
                        Lang.getAllLangs().toPref()
                    }
                )
            }

            if (oldVersion < 29) {
                deleteDataStore("anime_source_GogoAnime",context)
                deleteDataStore("anime_source_AnimeSama",context)
                deleteDataStore("anime_source_VostFree",context)
            }
        }
    }

    /* Preferences function for APP datastore*/
    private suspend fun deletePreferences(
        dataStore: DataStore<Preferences>,
        preferences: Set<Preferences.Key<*>>
    ) {
        dataStore.clearPreferences(preferences)
    }

    private suspend fun deleteAllPreferences(
        dataStore: DataStore<Preferences>,
    ) {
        dataStore.clearAllPreferences()
    }

    private suspend fun replacePreferences(
        dataStore: DataStore<Preferences>,
        filterPredicate: (Map.Entry<Preferences.Key<*>, Any?>) -> Boolean,
        newValue: (Any) -> Any = { it },
        newKey: (Preferences.Key<*>) -> String,
    ) {
        dataStore
            .getDataStoreValues()
            .asMap()
            .filter(filterPredicate)
            .forEach { (key, value) ->
                when (value) {
                    is Int -> {
                        dataStore.createPreference(
                            intPreferencesKey(newKey(key)),
                            newValue(value) as Int
                        )
                        dataStore.clearPreferences(setOf(key))
                    }

                    is Long -> {
                        dataStore.createPreference(
                            longPreferencesKey(newKey(key)),
                            newValue(value) as Long
                        )
                        dataStore.clearPreferences(setOf(key))
                    }

                    is Float -> {
                        dataStore.createPreference(
                            floatPreferencesKey(newKey(key)),
                            newValue(value) as Float
                        )
                        dataStore.clearPreferences(setOf(key))
                    }

                    is String -> {
                        dataStore.createPreference(
                            stringPreferencesKey(newKey(key)),
                            newValue(value) as String
                        )
                        dataStore.clearPreferences(setOf(key))
                    }

                    is Boolean -> {
                        dataStore.createPreference(
                            booleanPreferencesKey(newKey(key)),
                            newValue(value) as Boolean
                        )
                        dataStore.clearPreferences(setOf(key))
                    }

                    is Set<*> -> (value as? Set<String>)?.let {
                        dataStore.createPreference(
                            stringSetPreferencesKey(newKey(key)),
                            newValue(value) as Set<String>
                        )
                        dataStore.clearPreferences(setOf(key))
                    }
                }
            }
    }

    /* Preferences functions for SOURCE datastore*/
    private suspend fun deleteSourcePreferences(
        sourceId: Long,
        sourcesManager: SourceManager,
        preferences: Set<Preferences.Key<*>>
    ) {
        val source =
            (sourcesManager.getOrStub(sourceId)) as ConfigurableParsedHttpAnimeSource<*>
        deletePreferences(source.dataStore, preferences)
    }

    private suspend fun deleteAllSourcePreferences(
        sourceId: Long,
        sourcesManager: SourceManager,
    ) {
        val source =
            (sourcesManager.getOrStub(sourceId)) as ConfigurableParsedHttpAnimeSource<*>
        source.dataStore.clearAllPreferences()
    }

    private suspend fun replaceSourcePreferences(
        sourceId: Long,
        sourcesManager: SourceManager,
        filterPredicate: (Map.Entry<Preferences.Key<*>, Any?>) -> Boolean,
        newValue: (Any) -> Any = { it },
        newKey: (Preferences.Key<*>) -> String,
    ) {
        val source =
            (sourcesManager.getOrStub(sourceId)) as ConfigurableParsedHttpAnimeSource<*>
        replacePreferences(source.dataStore, filterPredicate, newValue, newKey)
    }

    /* Preferences functions for ALL datastore*/
    private fun deleteDataStore(
        dataStoreFileName: String,
        context: Context
    ) {
        val dataStoreDir = File(context.filesDir, "datastore")
        val dataStoreFile = File(dataStoreDir, "$dataStoreFileName.preferences_pb")
        if (dataStoreFile.exists()) {
            val deleted = dataStoreFile.delete()
            if (!deleted) Log.e(
                "Migrations errors",
                "Unable to delete dataStore file ${dataStoreFile.name}"
            )
        }
    }
}
