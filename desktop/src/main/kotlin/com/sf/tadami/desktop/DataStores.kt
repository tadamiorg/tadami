package com.sf.tadami.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** The preference files, one data store each: the app's and each configurable source's. */
object DataStores {

    private val stores = ConcurrentHashMap<String, DataStore<Preferences>>()

    fun get(fileName: String): DataStore<Preferences> = stores.computeIfAbsent(fileName) {
        PreferenceDataStoreFactory.create { File(AppDirs.preferences, "$fileName.preferences_pb") }
    }
}
