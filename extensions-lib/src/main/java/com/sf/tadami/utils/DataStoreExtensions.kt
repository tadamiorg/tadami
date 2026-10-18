package com.sf.tadami.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import kotlinx.coroutines.flow.Flow

/**
 * Every preference stored now.
 *
 * @since extensions-lib 1.5
 */
suspend fun DataStore<Preferences>.getDataStoreValues() : Preferences = throw Exception("Stub")

/**
 * Removes every preference.
 *
 * @since extensions-lib 1.5
 */
suspend fun DataStore<Preferences>.clearAllPreferences() : Unit = throw Exception("Stub")

/**
 * Removes the preferences [names].
 *
 * @since extensions-lib 1.5
 */
suspend fun DataStore<Preferences>.clearPreferences(names: Set<Preferences.Key<*>>) : Unit = throw Exception("Stub")

/**
 * The group of preferences [prefsGroup] reads, now.
 *
 * @since extensions-lib 1.5
 */
suspend fun <T : CustomPreferencesIdentifier> DataStore<Preferences>.getPreferencesGroup(prefsGroup: CustomPreferences<T>) : T = throw Exception("Stub")

/**
 * The group of preferences [prefsGroup] reads, each time a preference changes.
 *
 * @since extensions-lib 1.5
 */
fun <T : CustomPreferencesIdentifier> DataStore<Preferences>.getPreferencesGroupAsFlow(prefsGroup: CustomPreferences<T>) : Flow<T> = throw Exception("Stub")

/**
 * Writes the group [newValue] with [preferences], then calls [callBack] with it.
 *
 * @since extensions-lib 1.5
 */
suspend fun <T : CustomPreferencesIdentifier> DataStore<Preferences>.editPreferences(
    newValue: T,
    preferences: CustomPreferences<T>,
    callBack : (newValue : T) -> Unit = {}
) : Unit = throw Exception("Stub")

/**
 * Sets the preference [preferenceKey] to [newValue].
 *
 * @since extensions-lib 1.5
 */
suspend fun <T> DataStore<Preferences>.editPreference(
    newValue: T,
    preferenceKey: Preferences.Key<T>,
) : Unit = throw Exception("Stub")

/**
 * Sets the preference [key] to [value].
 *
 * @since extensions-lib 1.5
 */
suspend fun <T> DataStore<Preferences>.createPreference(
    key: Preferences.Key<T>,
    value : T,
) : Unit = throw Exception("Stub")

/**
 * Moves the preferences [filterPredicate] accepts to the key [newKey] gives, with the value
 * [newValue] gives: to migrate the preferences of an older version of a source.
 *
 * @since extensions-lib 1.5
 */
suspend fun DataStore<Preferences>.replacePreferences(
    filterPredicate: (Map.Entry<Preferences.Key<*>, Any?>) -> Boolean,
    newValue: (Any) -> Any = { it },
    newKey: (Preferences.Key<*>) -> String,
) : Unit = throw Exception("Stub")
