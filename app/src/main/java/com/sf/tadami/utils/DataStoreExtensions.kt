package com.sf.tadami.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

suspend fun DataStore<Preferences>.getDataStoreValues() =
    this.data.map { preferences ->
        preferences
    }.first()

suspend fun DataStore<Preferences>.clearAllPreferences() {
    this.edit { preferences ->
        preferences.clear()
    }
}
suspend fun DataStore<Preferences>.clearPreferences(names: Set<Preferences.Key<*>>) {
    this.edit { preferences ->
        names.forEach {
            preferences.remove(it)
        }
    }
}

suspend fun <T : CustomPreferencesIdentifier> DataStore<Preferences>.getPreferencesGroup(prefsGroup: CustomPreferences<T>) =
    this.data.map { preferences ->
        prefsGroup.transform(preferences)
    }.first()

fun <T : CustomPreferencesIdentifier> DataStore<Preferences>.getPreferencesGroupAsFlow(prefsGroup: CustomPreferences<T>) =
    this.data.map { preferences ->
        prefsGroup.transform(preferences)
    }

suspend fun <T : CustomPreferencesIdentifier> DataStore<Preferences>.editPreferences(
    newValue: T,
    preferences: CustomPreferences<T>,
    callBack : (newValue : T) -> Unit = {}
) {
    this.edit { prefs ->
        preferences.setPrefs(newValue,prefs)
        callBack(newValue)
    }
}

suspend fun <T> DataStore<Preferences>.editPreference(
    newValue: T,
    preferenceKey: Preferences.Key<T>,
) {
    this.edit { prefs ->
        prefs[preferenceKey] = newValue
    }
}
suspend fun <T> DataStore<Preferences>.createPreference(
    key: Preferences.Key<T>,
    value : T,
) {
    this.edit { prefs ->
        prefs[key] = value
    }
}