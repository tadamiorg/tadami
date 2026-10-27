package com.sf.tadami

import java.io.File
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/** The data stores of the sources, each in the file [fileOf] gives for its name. */
class DataStoresHandler(private val fileOf: (fileName: String) -> File) {
    private val dataStores: MutableMap<Long, DataStore<Preferences>> = mutableMapOf()
    private val dataStoresJobs : MutableMap<Long,CoroutineScope> = mutableMapOf()

    // Synchronized: sources resolve their data store from several threads at once, and a lost
    // entry would create a second DataStore for the same file
    @Synchronized
    fun removeDataStore(id: Long){
        dataStoresJobs.remove(id)?.cancel()
        dataStores.remove(id)
    }

    @Synchronized
    fun getDataStore(id: Long, fileName: String): DataStore<Preferences> {
        return dataStores.getOrPut(id) {
            val job = CoroutineScope(Dispatchers.IO + SupervisorJob())
            dataStoresJobs[id] = job
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler(
                    produceNewData = { emptyPreferences() }
                ),
                scope = job,
                produceFile = {
                    fileOf(fileName)
                }
            )
        }
    }
}