package com.sf.tadami.animesources.sources.en.gogoanime

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sf.tadami.ui.tabs.settings.model.CustomPreferences
import com.sf.tadami.ui.tabs.settings.model.CustomPreferencesIdentifier

data class GogoAnimePreferences(
    val baseUrl: String,
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<GogoAnimePreferences> {
        const val DEFAULT_BASE_URL = "https://anitaku.to"
        private val BASE_URL = stringPreferencesKey("base_url")

        override fun transform(preferences: Preferences): GogoAnimePreferences {
            return GogoAnimePreferences(
                baseUrl = preferences[BASE_URL] ?: DEFAULT_BASE_URL)
        }

        override fun setPrefs(newValue: GogoAnimePreferences, preferences: MutablePreferences) {
            preferences[BASE_URL] = newValue.baseUrl
        }
    }
}