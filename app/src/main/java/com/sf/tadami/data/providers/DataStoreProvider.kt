package com.sf.tadami.data.providers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.sf.tadami.ui.tabs.settings.model.CustomPreferences
import com.sf.tadami.ui.tabs.settings.model.CustomPreferencesIdentifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreProvider(
    private val dataStore: DataStore<Preferences>
) {

    suspend fun getDataStoreValues() =
        dataStore.data.map { preferences ->
            preferences
        }.first()

    fun getDataStore() = dataStore

    suspend fun clearAllPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    suspend fun clearPreferences(names: Set<Preferences.Key<*>>) {
        dataStore.edit { preferences ->
            names.forEach {
                preferences.remove(it)
            }
        }
    }

    suspend fun <T : CustomPreferencesIdentifier> getPreferencesGroup(prefsGroup: CustomPreferences<T>) =
        dataStore.data.map { preferences ->
            prefsGroup.transform(preferences)
        }.first()

    suspend fun <T : CustomPreferencesIdentifier> editPreferences(
        newValue: T,
        preferences: CustomPreferences<T>,
        callBack : (newValue : T) -> Unit = {}
    ) {
        dataStore.edit { prefs ->
            preferences.setPrefs(newValue,prefs)
            callBack(newValue)
        }
    }

    suspend fun <T> editPreference(
        newValue: T,
        preferenceKey: Preferences.Key<T>,
    ) {
        dataStore.edit { prefs ->
            prefs[preferenceKey] = newValue
        }
    }
    suspend fun <T> createPreference(
        key: Preferences.Key<T>,
        value : T,
    ) {
        dataStore.edit { prefs ->
            prefs[key] = value
        }
    }
}