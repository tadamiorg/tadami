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
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-source preference stores. Extensions are now loaded in parallel and each one touches its
 * DataStore from its own thread, so every access here has to be safe against concurrent callers —
 * creating two DataStores over the same file would silently diverge.
 */
class DataStoresHandler {
    private val dataStores: MutableMap<Long, DataStore<Preferences>> = ConcurrentHashMap()
    private val dataStoresJobs : MutableMap<Long, CoroutineScope> = ConcurrentHashMap()

    @Synchronized
    fun removeDataStore(id: Long){
        dataStoresJobs.remove(id)?.cancel()
        dataStores.remove(id)
    }

    @Synchronized
    fun getDataStore(id: Long, fileName: String): DataStore<Preferences> {
        dataStores[id]?.let { return it }

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