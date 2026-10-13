package com.sf.tadami.preferences.appearance

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sf.tadami.ui.themes.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearancePreferencesTest {

    @Test
    fun unknownStoredNamesFallBackToTheDefaults() {
        val prefs = AppearancePreferences.transform(
            mutablePreferencesOf(
                stringPreferencesKey("theme_mode") to "SEPIA",
                stringPreferencesKey("app_theme") to "THEME_FROM_A_NEWER_BUILD",
            )
        )
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
        assertEquals(AppTheme.DEFAULT, prefs.appTheme)
    }

    @Test
    fun missingNamesAreTheDefaults() {
        val prefs = AppearancePreferences.transform(emptyPreferences())
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
        assertEquals(AppTheme.DEFAULT, prefs.appTheme)
    }

    @Test
    fun knownStoredNamesAreKept() {
        val prefs = AppearancePreferences.transform(
            mutablePreferencesOf(
                stringPreferencesKey("theme_mode") to ThemeMode.DARK.name,
                stringPreferencesKey("app_theme") to AppTheme.TAKO.name,
            )
        )
        assertEquals(ThemeMode.DARK, prefs.themeMode)
        assertEquals(AppTheme.TAKO, prefs.appTheme)
    }
}
