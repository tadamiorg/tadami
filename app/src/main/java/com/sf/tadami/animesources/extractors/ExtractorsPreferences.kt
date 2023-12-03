package com.sf.tadami.animesources.extractors

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sf.tadami.ui.tabs.settings.model.CustomPreferences
import com.sf.tadami.ui.tabs.settings.model.CustomPreferencesIdentifier

data class ExtractorsPreferences(
    val streamSbEndpoint: String,
) : CustomPreferencesIdentifier {
    companion object : CustomPreferences<ExtractorsPreferences> {
        const val DEFAULT_STREAMSB_ENDPOINT = "/sources16"

        val STREAMSB_ENDPOINT = stringPreferencesKey("extractors_streamsb_endpoint")

        override fun transform(preferences: Preferences): ExtractorsPreferences {
            return ExtractorsPreferences(
                streamSbEndpoint = preferences[STREAMSB_ENDPOINT] ?: DEFAULT_STREAMSB_ENDPOINT,
            )
        }

        override fun setPrefs(newValue: ExtractorsPreferences, preferences: MutablePreferences) {
            preferences[STREAMSB_ENDPOINT] = newValue.streamSbEndpoint
        }
    }
}
