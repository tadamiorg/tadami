package com.sf.tadami.preferences.advanced

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier
import com.sf.tadami.ui.webview.adblock.FilterList

/** The WebView ad blocker. The lists turned off are stored, so a list added later starts on. */
data class AdBlockPreferences(
    val enabled: Boolean,
    val disabledLists: Set<String>,
) : CustomPreferencesIdentifier {

    val lists: List<FilterList> get() = FilterList.entries.filter { it.id !in disabledLists }

    companion object : CustomPreferences<AdBlockPreferences> {
        private val ENABLED = booleanPreferencesKey("webview_adblock_enabled")
        private val DISABLED_LISTS = stringSetPreferencesKey("webview_adblock_disabled_lists")

        override fun transform(preferences: Preferences): AdBlockPreferences {
            return AdBlockPreferences(
                enabled = preferences[ENABLED] ?: true,
                disabledLists = preferences[DISABLED_LISTS] ?: emptySet(),
            )
        }

        override fun setPrefs(newValue: AdBlockPreferences, preferences: MutablePreferences) {
            preferences[ENABLED] = newValue.enabled
            preferences[DISABLED_LISTS] = newValue.disabledLists
        }
    }
}
