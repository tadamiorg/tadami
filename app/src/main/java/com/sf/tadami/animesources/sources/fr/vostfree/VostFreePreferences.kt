package com.sf.tadami.animesources.sources.fr.vostfree

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class VostFreePreferences(
    val baseUrl: String,
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<VostFreePreferences> {
        const val DEFAULT_BASE_URL = "https://vostfree.ws"
        private val BASE_URL = stringPreferencesKey("base_url")

        override fun transform(preferences: Preferences): VostFreePreferences {
            return VostFreePreferences(
                baseUrl = preferences[BASE_URL] ?: DEFAULT_BASE_URL)
        }

        override fun setPrefs(newValue: VostFreePreferences, preferences: MutablePreferences) {
            preferences[BASE_URL] = newValue.baseUrl
        }
    }
}