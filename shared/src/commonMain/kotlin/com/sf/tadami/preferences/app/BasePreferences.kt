package com.sf.tadami.preferences.app

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class BasePreferences(
    val onboardingComplete : Boolean,
    val downloadedOnly : Boolean,
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<BasePreferences> {
        val ONBOARDING_COMPLETE = booleanPreferencesKey(CustomPreferences.appStateKey("onboarding_complete"))
        // App state, not backed up: restoring a backup shouldn't hide the library
        val DOWNLOADED_ONLY = booleanPreferencesKey(CustomPreferences.appStateKey("downloaded_only"))

        override fun transform(preferences: Preferences): BasePreferences {
            return BasePreferences(
                onboardingComplete = preferences[ONBOARDING_COMPLETE] ?: false,
                downloadedOnly = preferences[DOWNLOADED_ONLY] ?: false,
            )
        }

        override fun setPrefs(newValue: BasePreferences, preferences: MutablePreferences) {
            preferences[ONBOARDING_COMPLETE] = newValue.onboardingComplete
            preferences[DOWNLOADED_ONLY] = newValue.downloadedOnly
        }
    }
}