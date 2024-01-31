package com.sf.tadami.preferences.advanced

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class AdvancedPreferences(
    val userAgent : String,
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<AdvancedPreferences> {
        private val USER_AGENT =  stringPreferencesKey("app_default_user_agent")

        override fun transform(preferences: Preferences): AdvancedPreferences {
           return AdvancedPreferences(
               userAgent = preferences[USER_AGENT] ?: NetworkHelper.DEFAULT_USER_AGENT,
           )
        }

        override fun setPrefs(newValue: AdvancedPreferences, preferences: MutablePreferences) {
            preferences[USER_AGENT] = newValue.userAgent
        }
    }
}