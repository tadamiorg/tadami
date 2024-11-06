package com.sf.tadami

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DataStoresHandler {
    private val dataStores: MutableMap<Long, DataStore<Preferences>> = mutableMapOf()
    private val dataStoresJobs : MutableMap<Long,CoroutineScope> = mutableMapOf()

    fun removeDataStore(id: Long){
        dataStoresJobs[id]?.cancel()
        dataStores.remove(id)
    }

    fun getDataStore(id: Long, fileName: String): DataStore<Preferences> {
        val dataStore = dataStores[id]
        if(dataStore != null) return dataStore
        val job = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataStoresJobs[id] = job
        return dataStores.getOrPut(id) {
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { emptyPreferences() }
                ),
                scope = job,
                produceFile = {
                    Injekt.get<Application>().preferencesDataStoreFile(fileName)
                }
            )
        }
    }
}