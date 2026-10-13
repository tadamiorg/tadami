package com.sf.tadami.preferences.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun <T> rememberDataStoreState(
    customPrefs: CustomPreferences<T>,
    customDataStore: DataStore<Preferences>? = null
): DataStoreState<T> {
    val dataStore: DataStore<Preferences> = customDataStore ?: Injekt.get()
    val state = remember(dataStore) {
        DataStoreState(
            dataStore = dataStore,
            customPrefs = customPrefs
        )
    }
    // Follow the changes only while in the composition, a collector that outlives it leaks
    LaunchedEffect(state) {
        state.collectChanges()
    }
    return state
}

@Composable
fun rememberUnknownDataStoreState(
    unknownDataStore: DataStore<Preferences>
): UnknownDataStoreState {
    val dataStore: DataStore<Preferences> = unknownDataStore
    val state = remember(dataStore) {
        UnknownDataStoreState(
            dataStore = dataStore,
        )
    }
    LaunchedEffect(state) {
        state.collectChanges()
    }
    return state
}

class UnknownDataStoreState(
    private val dataStore: DataStore<Preferences>,
) {

    private val _value = MutableStateFlow(
        runBlocking {
            dataStore.data.first()
        }
    )
    val value: StateFlow<Preferences> = _value.asStateFlow()

    fun <T> setValue(newValue: T, key: Preferences.Key<T>) {
        runBlocking {
            dataStore.edit { preferences ->
                preferences[key] = newValue
            }
        }
    }

    internal suspend fun collectChanges() = withContext(Dispatchers.IO) {
        dataStore.data
            .collectLatest { prefs ->
                _value.update { prefs }
            }
    }
}


class DataStoreState<T>(
    private val dataStore: DataStore<Preferences>,
    private val customPrefs: CustomPreferences<T>,
) {

    private val _value = MutableStateFlow(
        runBlocking {
            dataStore.data.map { preferences ->
                customPrefs.transform(preferences)
            }.first()
        }
    )
    val value: StateFlow<T> = _value.asStateFlow()

    fun setValue(newValue: T) {
        // Only the keys that differ from the current value, or aren't stored yet: writing the whole group
        // from this copy could revert a key written meanwhile elsewhere, like the library update worker's count
        val oldPrefs = mutablePreferencesOf().also { customPrefs.setPrefs(_value.value, it) }.asMap()
        val newPrefs = mutablePreferencesOf().also { customPrefs.setPrefs(newValue, it) }.asMap()
        runBlocking {
            dataStore.edit { preferences ->
                oldPrefs.keys.filter { it !in newPrefs }.forEach { preferences.remove(it) }
                newPrefs.filter { (key, value) ->
                    oldPrefs[key] != value || key !in preferences
                }.forEach { (key, value) ->
                    @Suppress("UNCHECKED_CAST")
                    preferences[key as Preferences.Key<Any>] = value
                }
            }
        }
    }

    internal suspend fun collectChanges() = withContext(Dispatchers.IO) {
        dataStore.data
            .map { preferences ->
                customPrefs.transform(preferences)
            }
            .collectLatest { prefs ->
                _value.update { prefs }
            }
    }
}