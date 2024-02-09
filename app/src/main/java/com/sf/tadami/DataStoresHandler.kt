package com.sf.tadami

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DataStoresHandler {
    private val dataStores: MutableMap<Long, DataStore<Preferences>> = mutableMapOf()
    fun getDataStore(id: Long, fileName: String): DataStore<Preferences> {
        return dataStores.getOrPut(id) {
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { emptyPreferences() }
                ),
                migrations = listOf(
                    SharedPreferencesMigration(
                        Injekt.get<Application>(),
                        fileName
                    )
                ),
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                produceFile = {
                    Injekt.get<Application>().preferencesDataStoreFile(fileName)
                }
            )
        }

    }
}