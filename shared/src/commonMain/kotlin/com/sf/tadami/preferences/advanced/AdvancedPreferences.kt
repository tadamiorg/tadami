package com.sf.tadami.preferences.advanced

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.utils.UserAgents
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class AdvancedPreferences(
    val userAgent : String,
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<AdvancedPreferences> {
        private val USER_AGENT =  stringPreferencesKey("app_default_user_agent")

        override fun transform(preferences: Preferences): AdvancedPreferences {
           return AdvancedPreferences(
               // No value, or a default persisted by an older version: follow the WebView version
               userAgent = preferences[USER_AGENT]?.trim()
                   ?.takeUnless { it.isEmpty() || UserAgents.isLegacyDefault(it) }
                   ?: NetworkHelper.defaultUserAgent,
           )
        }

        override fun setPrefs(newValue: AdvancedPreferences, preferences: MutablePreferences) {
            val userAgent = newValue.userAgent.trim()
            if (userAgent.isEmpty() || userAgent == NetworkHelper.defaultUserAgent) {
                // Keep following the WebView version instead of freezing today's default
                preferences.remove(USER_AGENT)
            } else {
                preferences[USER_AGENT] = userAgent
            }
        }
    }
}
