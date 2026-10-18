package com.sf.tadami.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import kotlinx.coroutines.flow.Flow

suspend fun DataStore<Preferences>.getDataStoreValues() : Preferences = throw Exception("Stub")

suspend fun DataStore<Preferences>.clearAllPreferences() : Unit = throw Exception("Stub")
suspend fun DataStore<Preferences>.clearPreferences(names: Set<Preferences.Key<*>>) : Unit = throw Exception("Stub")

suspend fun <T : CustomPreferencesIdentifier> DataStore<Preferences>.getPreferencesGroup(prefsGroup: CustomPreferences<T>) : T = throw Exception("Stub")

fun <T : CustomPreferencesIdentifier> DataStore<Preferences>.getPreferencesGroupAsFlow(prefsGroup: CustomPreferences<T>) : Flow<T> = throw Exception("Stub")

suspend fun <T : CustomPreferencesIdentifier> DataStore<Preferences>.editPreferences(
    newValue: T,
    preferences: CustomPreferences<T>,
    callBack : (newValue : T) -> Unit = {}
) : Unit = throw Exception("Stub")

suspend fun <T> DataStore<Preferences>.editPreference(
    newValue: T,
    preferenceKey: Preferences.Key<T>,
) : Unit = throw Exception("Stub")
suspend fun <T> DataStore<Preferences>.createPreference(
    key: Preferences.Key<T>,
    value : T,
) : Unit = throw Exception("Stub")

suspend fun DataStore<Preferences>.replacePreferences(
    filterPredicate: (Map.Entry<Preferences.Key<*>, Any?>) -> Boolean,
    newValue: (Any) -> Any = { it },
    newKey: (Preferences.Key<*>) -> String,
) : Unit = throw Exception("Stub")