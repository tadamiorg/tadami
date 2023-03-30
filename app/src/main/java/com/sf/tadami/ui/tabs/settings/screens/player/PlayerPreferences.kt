package com.sf.tadami.ui.tabs.settings.screens.player

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.sf.tadami.ui.tabs.settings.model.CustomPreferences

data class PlayerPreferences(
    val seenThreshold : Int,
    val doubleTapLength : Long
) {
    companion object : CustomPreferences<PlayerPreferences>{
        const val DEFAULT_SEEN_THRESHOLD = 85
        const val DEFAULT_DOUBLE_TAP_LENGTH = 10000L

        val SEEN_THRESHOLD =  intPreferencesKey("player_seen_threshold")
        val DOUBLE_TAP_LENGTH = longPreferencesKey("player_double_tap_length")

        override fun transform(preferences: Preferences): PlayerPreferences {
           return PlayerPreferences(
               seenThreshold = preferences[SEEN_THRESHOLD] ?: DEFAULT_SEEN_THRESHOLD,
               doubleTapLength = preferences[DOUBLE_TAP_LENGTH] ?: DEFAULT_DOUBLE_TAP_LENGTH
           )
        }

        override fun setPrefs(newValue: PlayerPreferences, preferences: MutablePreferences) {
            preferences[SEEN_THRESHOLD] = newValue.seenThreshold
            preferences[DOUBLE_TAP_LENGTH] = newValue.doubleTapLength
        }
    }
}