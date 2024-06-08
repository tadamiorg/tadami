package com.sf.tadami.preferences.app

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class UpdatePreferences(
    val lastUpdatedCheckTimestamp : Long,
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<UpdatePreferences> {
        val APP_UPDATE_CHECK_LAST_TIMESTAMP = longPreferencesKey(CustomPreferences.appStateKey("app_update_check_last_timestamp"))

        override fun transform(preferences: Preferences): UpdatePreferences {
            return UpdatePreferences(
                lastUpdatedCheckTimestamp = preferences[APP_UPDATE_CHECK_LAST_TIMESTAMP] ?: 0L,
            )
        }

        override fun setPrefs(newValue: UpdatePreferences, preferences: MutablePreferences) {
            preferences[APP_UPDATE_CHECK_LAST_TIMESTAMP] = newValue.lastUpdatedCheckTimestamp
        }
    }
}