package com.sf.tadami

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.sf.tadami.data.providers.DataStoreProvider
import com.sf.tadami.notifications.backup.BackupCreateWorker
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateWorker
import com.sf.tadami.ui.tabs.settings.externalpreferences.source.SourcesPreferences
import com.sf.tadami.ui.tabs.settings.screens.backup.BackupPreferences
import com.sf.tadami.ui.tabs.settings.screens.library.LibraryPreferences
import com.sf.tadami.ui.tabs.settings.screens.player.PlayerPreferences

object Migrations {

    /**
     * Performs a migration when the application is updated.
     *
     * @return true if a migration is performed, false otherwise.
     */
    suspend fun upgrade(
        context: Context,
        dataStoreProvider: DataStoreProvider,
        appPreferences: AppPreferences,
        libraryPreferences: LibraryPreferences,
        playerPreferences: PlayerPreferences,
        backupPreferences: BackupPreferences,
        sourcesPreferences: SourcesPreferences
    ) {
        val oldVersion = appPreferences.lastVersionCode
        if (oldVersion < BuildConfig.VERSION_CODE) {
            dataStoreProvider.editPreference(
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
            if (oldVersion < 18) {
                val oldAutoUpdateRestrictions = libraryPreferences.autoUpdateRestrictions
                val oldAutoUpdateInterval = libraryPreferences.autoUpdateInterval

                val newAutoUpdateRestrictions = when {
                    oldAutoUpdateRestrictions.isEmpty() -> {
                        setOf(
                            LibraryPreferences.AutoUpdateRestrictionItems.WIFI
                        )
                    }
                    else -> {
                        val mutableSet = oldAutoUpdateRestrictions.toMutableSet()
                        mutableSet.removeAll(listOf("cellular"))
                        if(mutableSet.isEmpty()){
                            setOf(
                                LibraryPreferences.AutoUpdateRestrictionItems.WIFI
                            )
                        }else{
                            mutableSet
                        }
                    }

                }

                val prefsToReplace = mapOf(
                    intPreferencesKey("library_auto_updates") to ("library_auto_update_interval" to oldAutoUpdateInterval),
                    stringSetPreferencesKey("library_update_restrictions") to ("library_auto_update_restrictions" to newAutoUpdateRestrictions)
                )
                replacePreferences(
                    dataStoreProvider = dataStoreProvider,
                    filterPredicate = { it.key in prefsToReplace.keys },
                    newKey = { prefsToReplace[it]!!.first },
                    newValue = { prefsToReplace[it]!!.second }
                )

            }
        }
    }

    private suspend fun deletePreferences(
        dataStoreProvider: DataStoreProvider,
        preferences: Set<Preferences.Key<*>>
    ) {
        dataStoreProvider.clearPreferences(preferences)
    }

    private suspend fun replacePreferences(
        dataStoreProvider: DataStoreProvider,
        filterPredicate: (Map.Entry<Preferences.Key<*>, Any?>) -> Boolean,
        newValue : (Any) -> Any = {it},
        newKey: (Preferences.Key<*>) -> String,
    ) {
        dataStoreProvider
            .getDataStoreValues()
            .asMap()
            .filter(filterPredicate)
            .forEach { (key, value) ->
                when (value) {
                    is Int -> {
                        dataStoreProvider.createPreference(
                            intPreferencesKey(newKey(key)),
                            newValue(value) as Int
                        )
                        dataStoreProvider.clearPreferences(setOf(key))
                    }

                    is Long -> {
                        dataStoreProvider.createPreference(
                            longPreferencesKey(newKey(key)),
                            newValue(value) as Long
                        )
                        dataStoreProvider.clearPreferences(setOf(key))
                    }

                    is Float -> {
                        dataStoreProvider.createPreference(
                            floatPreferencesKey(newKey(key)),
                            newValue(value) as Float
                        )
                        dataStoreProvider.clearPreferences(setOf(key))
                    }

                    is String -> {
                        dataStoreProvider.createPreference(
                            stringPreferencesKey(newKey(key)),
                            newValue(value) as String
                        )
                        dataStoreProvider.clearPreferences(setOf(key))
                    }

                    is Boolean -> {
                        dataStoreProvider.createPreference(
                            booleanPreferencesKey(newKey(key)),
                            newValue(value) as Boolean
                        )
                        dataStoreProvider.clearPreferences(setOf(key))
                    }

                    is Set<*> -> (value as? Set<String>)?.let {
                        dataStoreProvider.createPreference(
                            stringSetPreferencesKey(newKey(key)),
                            newValue(value) as Set<String>
                        )
                        dataStoreProvider.clearPreferences(setOf(key))
                    }
                }
            }
    }
}