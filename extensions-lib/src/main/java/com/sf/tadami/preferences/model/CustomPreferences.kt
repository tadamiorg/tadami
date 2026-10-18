package com.sf.tadami.preferences.model

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences

interface CustomPreferences<T> {
    fun transform(preferences: Preferences) : T
    fun setPrefs(newValue : T,preferences : MutablePreferences)

    companion object{
        fun isPrivate(key: String): Boolean = throw Exception("Stub")
        fun privateKey(key: String): String = throw Exception("Stub")

        fun isAppState(key: String): Boolean = throw Exception("Stub")
        fun appStateKey(key: String): String = throw Exception("Stub")
        private const val APP_STATE_PREFIX = "__APP_STATE_"
        private const val PRIVATE_PREFIX = "__PRIVATE_"
    }
}

interface CustomPreferencesIdentifier {}