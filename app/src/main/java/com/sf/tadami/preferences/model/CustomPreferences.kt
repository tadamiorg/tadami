package com.sf.tadami.preferences.model

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences

interface CustomPreferences<T> {
    fun transform(preferences: Preferences) : T
    fun setPrefs(newValue : T,preferences : MutablePreferences)

    companion object{
        fun isPrivate(key: String): Boolean {
            return key.startsWith(PRIVATE_PREFIX)
        }
        fun privateKey(key: String): String {
            return "$PRIVATE_PREFIX$key"
        }

        fun isAppState(key: String): Boolean {
            return key.startsWith(APP_STATE_PREFIX)
        }
        fun appStateKey(key: String): String {
            return "$APP_STATE_PREFIX$key"
        }

        private const val APP_STATE_PREFIX = "__APP_STATE_"
        private const val PRIVATE_PREFIX = "__PRIVATE_"
    }
}

interface CustomPreferencesIdentifier {}