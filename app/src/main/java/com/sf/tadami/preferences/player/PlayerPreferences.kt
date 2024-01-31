package com.sf.tadami.preferences.player

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.sf.tadami.preferences.model.CustomPreferences
import com.sf.tadami.preferences.model.CustomPreferencesIdentifier

data class PlayerPreferences(
    val seenThreshold : Int,
    val doubleTapLength : Long,
    val autoPlay : Boolean
) : CustomPreferencesIdentifier {

    object SeenThresholdItems {
        const val SEVENTY = 70
        const val SEVENTY_FIVE = 75
        const val EIGHTY = 80
        const val EIGHTY_FIVE = 85
        const val NINETY = 90
        const val NINETY_FIVE = 95
        const val HUNDRED = 100
    }
    object DoubleTapLengthItems {
        const val FIVE = 5000L
        const val TEN = 10000L
        const val FIFTEEN = 15000L
        const val TWENTY = 20000L
        const val TWENTY_FIVE = 25000L
        const val THIRTY = 30000L
    }
    companion object : CustomPreferences<PlayerPreferences> {
        private val SEEN_THRESHOLD =  intPreferencesKey("player_seen_threshold")
        private val DOUBLE_TAP_LENGTH = longPreferencesKey("player_double_tap_length")
        private val AUTO_PLAY = booleanPreferencesKey("player_auto_play")

        override fun transform(preferences: Preferences): PlayerPreferences {
           return PlayerPreferences(
               seenThreshold = preferences[SEEN_THRESHOLD] ?: SeenThresholdItems.EIGHTY_FIVE,
               doubleTapLength = preferences[DOUBLE_TAP_LENGTH] ?: DoubleTapLengthItems.TEN,
               autoPlay = preferences[AUTO_PLAY] ?: false
           )
        }

        override fun setPrefs(newValue: PlayerPreferences, preferences: MutablePreferences) {
            preferences[SEEN_THRESHOLD] = newValue.seenThreshold
            preferences[DOUBLE_TAP_LENGTH] = newValue.doubleTapLength
            preferences[AUTO_PLAY] = newValue.autoPlay
        }
    }
}