package com.sf.tadami.preferences.download

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class DownloadPreferences(
    val onlyOverWifi: Boolean,
    val parallelDownloads: Int,
    val deleteAfterWatched: Boolean,
) : CustomPreferencesIdentifier {

    object ParallelDownloadsItems {
        const val MIN = 1
        const val DEFAULT = 2
        const val MAX = 5
    }

    companion object : CustomPreferences<DownloadPreferences> {
        val ONLY_OVER_WIFI = booleanPreferencesKey("download_only_over_wifi")
        val PARALLEL_DOWNLOADS = intPreferencesKey("download_parallel")
        val DELETE_AFTER_WATCHED = booleanPreferencesKey("download_delete_after_watched")

        override fun transform(preferences: Preferences): DownloadPreferences {
            return DownloadPreferences(
                onlyOverWifi = preferences[ONLY_OVER_WIFI] ?: true,
                parallelDownloads = (preferences[PARALLEL_DOWNLOADS] ?: ParallelDownloadsItems.DEFAULT)
                    .coerceIn(ParallelDownloadsItems.MIN, ParallelDownloadsItems.MAX),
                deleteAfterWatched = preferences[DELETE_AFTER_WATCHED] ?: false,
            )
        }

        override fun setPrefs(newValue: DownloadPreferences, preferences: MutablePreferences) {
            preferences[ONLY_OVER_WIFI] = newValue.onlyOverWifi
            preferences[PARALLEL_DOWNLOADS] = newValue.parallelDownloads
            preferences[DELETE_AFTER_WATCHED] = newValue.deleteAfterWatched
        }
    }
}
