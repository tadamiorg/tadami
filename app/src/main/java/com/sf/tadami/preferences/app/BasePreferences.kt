package com.sf.tadami.preferences.app

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class BasePreferences(
    val onboardingComplete : Boolean,
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<BasePreferences> {
        val ONBOARDING_COMPLETE = booleanPreferencesKey(CustomPreferences.appStateKey("onboarding_complete"))

        override fun transform(preferences: Preferences): BasePreferences {
            return BasePreferences(
                onboardingComplete = preferences[ONBOARDING_COMPLETE] ?: false,
            )
        }

        override fun setPrefs(newValue: BasePreferences, preferences: MutablePreferences) {
            preferences[ONBOARDING_COMPLETE] = newValue.onboardingComplete
        }
    }
}