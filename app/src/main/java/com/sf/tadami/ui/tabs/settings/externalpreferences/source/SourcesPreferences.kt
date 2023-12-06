package com.sf.tadami.ui.tabs.settings.externalpreferences.source

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.sf.tadami.ui.tabs.settings.model.CustomPreferences
import com.sf.tadami.ui.tabs.settings.model.CustomPreferencesIdentifier
import com.sf.tadami.utils.Lang
import com.sf.tadami.utils.Lang.Companion.toPref

data class SourcesPreferences(
    val hiddenSources : Set<String>,
    val enabledLanguages : Set<String>
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<SourcesPreferences> {
        private val HIDDEN_SOURCES =  stringSetPreferencesKey("hidden_sources")
        val ENABLED_LANGUAGES =  stringSetPreferencesKey("enabled_languages")

        override fun transform(preferences: Preferences): SourcesPreferences {
            return SourcesPreferences(
                hiddenSources = preferences[HIDDEN_SOURCES] ?: emptySet(),
                enabledLanguages = preferences[ENABLED_LANGUAGES] ?: Lang.getAllLangs().toPref()
            )
        }

        override fun setPrefs(newValue: SourcesPreferences, preferences: MutablePreferences) {
            preferences[HIDDEN_SOURCES] = newValue.hiddenSources
            preferences[ENABLED_LANGUAGES] = newValue.enabledLanguages
        }
    }
}