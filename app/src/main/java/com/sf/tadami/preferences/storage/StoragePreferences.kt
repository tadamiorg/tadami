package com.sf.tadami.preferences.storage

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sf.tadami.data.providers.AndroidFoldersProvider
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class StoragePreferences(
    val storageDir: String,
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<StoragePreferences> {
        val STORAGE_DIR = stringPreferencesKey("storage_dir")

        override fun transform(preferences: Preferences): StoragePreferences {
            val foldersProviders: AndroidFoldersProvider = Injekt.get()
            return StoragePreferences(
                storageDir = preferences[STORAGE_DIR] ?: foldersProviders.path()
            )
        }

        override fun setPrefs(newValue: StoragePreferences, preferences: MutablePreferences) {
            preferences[STORAGE_DIR] = newValue.storageDir
        }
    }
}