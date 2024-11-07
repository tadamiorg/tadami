package com.sf.tadami.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import kotlinx.coroutines.flow.Flow
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

fun DataStore<Preferences>.isSet(key: Preferences.Key<*>) : Flow<Boolean> =
    this.data.map { preferences ->
        preferences.contains(key)
    }

fun <T : CustomPreferencesIdentifier> DataStore<Preferences>.getPreferencesGroupAsFlow(prefsGroup: CustomPreferences<T>) =
    this.data.map { preferences ->
        prefsGroup.transform(preferences)
    }

suspend fun <T : CustomPreferencesIdentifier> DataStore<Preferences>.editPreferences(
    newValue: T,
    preferences: CustomPreferences<T>,
    callBack : (newValue : T) -> Unit = {}
) : Preferences {
    return this.edit { prefs ->
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

suspend fun DataStore<Preferences>.replacePreferences(
    filterPredicate: (Map.Entry<Preferences.Key<*>, Any?>) -> Boolean,
    newValue: (Any) -> Any = { it },
    newKey: (Preferences.Key<*>) -> String,
) {
    this
        .getDataStoreValues()
        .asMap()
        .filter(filterPredicate)
        .forEach { (key, value) ->
            when (value) {
                is Int -> {
                    this.createPreference(
                        intPreferencesKey(newKey(key)),
                        newValue(value) as Int
                    )
                    this.clearPreferences(setOf(key))
                }

                is Long -> {
                    this.createPreference(
                        longPreferencesKey(newKey(key)),
                        newValue(value) as Long
                    )
                    this.clearPreferences(setOf(key))
                }

                is Float -> {
                    this.createPreference(
                        floatPreferencesKey(newKey(key)),
                        newValue(value) as Float
                    )
                    this.clearPreferences(setOf(key))
                }

                is String -> {
                    this.createPreference(
                        stringPreferencesKey(newKey(key)),
                        newValue(value) as String
                    )
                    this.clearPreferences(setOf(key))
                }

                is Boolean -> {
                    this.createPreference(
                        booleanPreferencesKey(newKey(key)),
                        newValue(value) as Boolean
                    )
                    this.clearPreferences(setOf(key))
                }

                is Set<*> -> (value as? Set<String>)?.let {
                    this.createPreference(
                        stringSetPreferencesKey(newKey(key)),
                        newValue(value) as Set<String>
                    )
                    this.clearPreferences(setOf(key))
                }
            }
        }
}