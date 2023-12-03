package com.sf.tadami.animesources.sources.fr.animesama

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sf.tadami.ui.tabs.settings.model.CustomPreferences
import com.sf.tadami.ui.tabs.settings.model.CustomPreferencesIdentifier

data class AnimeSamaPreferences(
    val baseUrl: String,
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<AnimeSamaPreferences> {
        const val DEFAULT_BASE_URL = "https://anime-sama.fr"
        private val BASE_URL = stringPreferencesKey("base_url")

        override fun transform(preferences: Preferences): AnimeSamaPreferences {
            return AnimeSamaPreferences(
                baseUrl = preferences[BASE_URL] ?: DEFAULT_BASE_URL)
        }

        override fun setPrefs(newValue: AnimeSamaPreferences, preferences: MutablePreferences) {
            preferences[BASE_URL] = newValue.baseUrl
        }
    }
}