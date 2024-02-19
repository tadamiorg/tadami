package com.sf.tadami.preferences.model

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun <T> rememberDataStoreState(
    customPrefs: CustomPreferences<T>,
    customDataStore: DataStore<Preferences>? = null
): DataStoreState<T> {
    val dataStore: DataStore<Preferences> = customDataStore ?: Injekt.get()
    return remember(dataStore) {
        DataStoreState(
            dataStore = dataStore,
            customPrefs = customPrefs
        )
    }
}

@Composable
fun rememberUnknownDataStoreState(
    unknownDataStore: DataStore<Preferences>
): UnknownDataStoreState {
    val dataStore: DataStore<Preferences> = unknownDataStore
    return remember(dataStore) {
        UnknownDataStoreState(
            dataStore = dataStore,
        )
    }
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

    init {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data
                .collectLatest { prefs ->
                    _value.update { prefs }
                }
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
        runBlocking {
            dataStore.edit { preferences ->
                customPrefs.setPrefs(newValue, preferences)
            }
        }
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data
                .map { preferences ->
                    customPrefs.transform(preferences)
                }
                .collectLatest { prefs ->
                    _value.update { prefs }
                }
        }
    }
}