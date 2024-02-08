package com.sf.tadami.preferences.sources

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import com.sf.tadami.utils.Lang
import com.sf.tadami.utils.Lang.Companion.toPref

data class SourcesPreferences(
    val hiddenSources : Set<String>,
    val enabledLanguages : Set<String>,
    val lastExtCheck : Long,
    val extensionUpdatesCount : Int,
    val extensionsRepos : Set<String>
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<SourcesPreferences> {
        private val HIDDEN_SOURCES =  stringSetPreferencesKey("hidden_sources")
        private val DEFAULT_EXTENSIONS_REPO = "https://raw.githubusercontent.com/tadamiorg/tadami-extensions/main"
        val ENABLED_LANGUAGES =  stringSetPreferencesKey("enabled_languages")
        val LAST_EXT_CHECK = longPreferencesKey(CustomPreferences.appStateKey("last_ext_check"))
        val EXTENSIONS_REPOS = stringSetPreferencesKey("extensions_repos")
        val EXT_UPDATES_COUNT = intPreferencesKey("ext_updates_count")

        override fun transform(preferences: Preferences): SourcesPreferences {
            return SourcesPreferences(
                hiddenSources = preferences[HIDDEN_SOURCES] ?: emptySet(),
                enabledLanguages = preferences[ENABLED_LANGUAGES] ?: Lang.getAllLangs().toPref(),
                lastExtCheck = preferences[LAST_EXT_CHECK] ?: 0,
                extensionUpdatesCount = preferences[EXT_UPDATES_COUNT] ?: 0,
                extensionsRepos = preferences[EXTENSIONS_REPOS] ?: setOf(DEFAULT_EXTENSIONS_REPO)
            )
        }

        override fun setPrefs(newValue: SourcesPreferences, preferences: MutablePreferences) {
            preferences[HIDDEN_SOURCES] = newValue.hiddenSources
            preferences[ENABLED_LANGUAGES] = newValue.enabledLanguages
            preferences[LAST_EXT_CHECK] = newValue.lastExtCheck
            preferences[EXT_UPDATES_COUNT] = newValue.extensionUpdatesCount
            preferences[EXTENSIONS_REPOS] = newValue.extensionsRepos
        }
    }
}