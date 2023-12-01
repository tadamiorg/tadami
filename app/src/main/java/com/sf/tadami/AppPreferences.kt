package com.sf.tadami

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.sf.tadami.ui.tabs.settings.model.CustomPreferences
import com.sf.tadami.ui.tabs.settings.model.CustomPreferencesIdentifier

data class AppPreferences(
    val lastVersionCode : Int
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<AppPreferences> {
        val LAST_VERSION_CODE = intPreferencesKey(CustomPreferences.appStateKey("last_version_code"))

        override fun transform(preferences: Preferences): AppPreferences {
            return AppPreferences(
                lastVersionCode = preferences[LAST_VERSION_CODE] ?: 0,
            )
        }

        override fun setPrefs(newValue: AppPreferences, preferences: MutablePreferences) {
            preferences[LAST_VERSION_CODE] = newValue.lastVersionCode
        }
    }
}