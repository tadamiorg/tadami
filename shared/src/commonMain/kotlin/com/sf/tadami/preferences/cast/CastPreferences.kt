package com.sf.tadami.preferences.cast

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class CastPreferences(
    /** Show the "you're casting to the web receiver" notice; users can opt out from the dialog. */
    val showWebReceiverNotice: Boolean,
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<CastPreferences> {
        private val SHOW_WEB_RECEIVER_NOTICE =
            booleanPreferencesKey(CustomPreferences.appStateKey("cast_show_web_receiver_notice"))

        override fun transform(preferences: Preferences): CastPreferences {
            return CastPreferences(
                showWebReceiverNotice = preferences[SHOW_WEB_RECEIVER_NOTICE] ?: true,
            )
        }

        override fun setPrefs(newValue: CastPreferences, preferences: MutablePreferences) {
            preferences[SHOW_WEB_RECEIVER_NOTICE] = newValue.showWebReceiverNotice
        }
    }
}
