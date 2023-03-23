package com.sf.tadami.ui.tabs.settings.model

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences

interface CustomPreferences<T> {
    fun transform(preferences: Preferences) : T
    fun setPrefs(newValue : T,preferences : MutablePreferences)
}