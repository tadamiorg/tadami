package com.sf.tadami.preferences.library

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class LibraryPreferences(
    val portraitColumns: Int,
    val landscapeColumns: Int,
    val autoUpdateInterval: Int,
    val autoUpdateRestrictions: Set<String>,
    val filterFlags: LibraryFilter,
    val sortFlags : LibrarySort,
    val lastUpdatedTimestamp : Long,
    val newUpdatesCount : Int
) : CustomPreferencesIdentifier {

    object AutoUpdateRestrictionItems {
        const val WIFI = "wifi"
        const val CHARGE = "charge"
        const val BATTERY = "battery"
    }

    object AutoUpdateIntervalItems {
        const val DISABLED = 0
        const val TWELVE = 12
        const val DAILY = 24
        const val DAILY_2 = 48
        const val DAILY_3 = 72
        const val WEEKLY = 168
    }

    companion object : CustomPreferences<LibraryPreferences> {
        private val PORTRAIT_COLUMNS = intPreferencesKey("library_portrait_columns")
        private val LANDSCAPE_COLUMNS = intPreferencesKey("library_landscape_columns")
        private val AUTO_UPDATE_INTERVAL = intPreferencesKey("library_auto_update_interval")
        private val AUTO_UPDATE_RESTRICTIONS = stringSetPreferencesKey("library_auto_update_restrictions")
        private val LIBRARY_UPDATE_LAST_TIMESTAMP = longPreferencesKey(CustomPreferences.appStateKey("library_update_last_timestamp"))
        private val NEW_UPDATES_COUNT = intPreferencesKey(CustomPreferences.appStateKey("library_unseen_updates_count"))

        // Flags
        private val FILTER_FLAGS = longPreferencesKey("library_filter_flags")
        private val SORT_FLAGS = longPreferencesKey("library_sort_flags")

        override fun transform(preferences: Preferences): LibraryPreferences {
            return LibraryPreferences(
                portraitColumns = preferences[PORTRAIT_COLUMNS] ?: 0,
                landscapeColumns = preferences[LANDSCAPE_COLUMNS] ?: 0,
                autoUpdateInterval = preferences[AUTO_UPDATE_INTERVAL] ?: AutoUpdateIntervalItems.DISABLED,
                autoUpdateRestrictions = preferences[AUTO_UPDATE_RESTRICTIONS] ?: setOf(
                    AutoUpdateRestrictionItems.WIFI
                ),
                filterFlags = LibraryFilter(flags = preferences[FILTER_FLAGS] ?: LibraryFilter.DEFAULT_FILTER),
                sortFlags = LibrarySort(flags = preferences[SORT_FLAGS] ?: LibrarySort.DEFAULT_SORT),
                lastUpdatedTimestamp = preferences[LIBRARY_UPDATE_LAST_TIMESTAMP] ?: 0L,
                newUpdatesCount = preferences[NEW_UPDATES_COUNT] ?: 0
            )
        }

        override fun setPrefs(newValue: LibraryPreferences, preferences: MutablePreferences) {
            preferences[PORTRAIT_COLUMNS] = newValue.portraitColumns
            preferences[LANDSCAPE_COLUMNS] = newValue.landscapeColumns
            preferences[AUTO_UPDATE_INTERVAL] = newValue.autoUpdateInterval
            preferences[AUTO_UPDATE_RESTRICTIONS] = newValue.autoUpdateRestrictions
            preferences[FILTER_FLAGS] = newValue.filterFlags.flags
            preferences[SORT_FLAGS] = newValue.sortFlags.flags
            preferences[LIBRARY_UPDATE_LAST_TIMESTAMP] = newValue.lastUpdatedTimestamp
            preferences[NEW_UPDATES_COUNT] = newValue.newUpdatesCount
        }


    }
}



