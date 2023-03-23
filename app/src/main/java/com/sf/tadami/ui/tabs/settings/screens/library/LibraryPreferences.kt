package com.sf.tadami.ui.tabs.settings.screens.library

import androidx.datastore.preferences.core.*
import com.sf.tadami.ui.tabs.settings.model.CustomPreferences

data class LibraryPreferences(
    val portraitColumns: Int,
    val landscapeColumns : Int,
    val autoUpdates : Int,
    val updateRestrictions : Set<String>
) {
    companion object : CustomPreferences<LibraryPreferences> {
        val PORTRAIT_COLUMNS = intPreferencesKey("library_portrait_columns")
        val LANDSCAPE_COLUMNS = intPreferencesKey("library_landscape_columns")
        val AUTO_UPDATES = intPreferencesKey("library_auto_updates")
        val UPDATE_RESTRICTIONS = stringSetPreferencesKey("library_update_restrictions")

        override fun transform(preferences: Preferences): LibraryPreferences {
            return LibraryPreferences(
                portraitColumns = preferences[PORTRAIT_COLUMNS] ?: 0,
                landscapeColumns = preferences[LANDSCAPE_COLUMNS] ?: 0,
                autoUpdates = preferences[AUTO_UPDATES] ?: 24,
                updateRestrictions = preferences[UPDATE_RESTRICTIONS] ?: setOf(UPDATE_RESTRICTIONS_ITEMS.WIFI)
            )
        }

        override fun setPrefs(newValue: LibraryPreferences, preferences: MutablePreferences) {
            preferences[PORTRAIT_COLUMNS] = newValue.portraitColumns
            preferences[LANDSCAPE_COLUMNS] = newValue.landscapeColumns
            preferences[AUTO_UPDATES] = newValue.autoUpdates
            preferences[UPDATE_RESTRICTIONS] = newValue.updateRestrictions
        }
    }
}