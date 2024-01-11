package com.sf.tadami.ui.tabs.settings.screens.appearance

import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sf.tadami.ui.tabs.settings.model.CustomPreferences
import com.sf.tadami.ui.tabs.settings.model.CustomPreferencesIdentifier
import com.sf.tadami.ui.themes.AppTheme

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

fun setAppCompatDelegateThemeMode(themeMode: ThemeMode) {
    AppCompatDelegate.setDefaultNightMode(
        when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        },
    )
}


data class AppearancePreferences(
    val themeMode: ThemeMode,
    val appTheme: AppTheme,
    val themeDarkAmoled : Boolean
) : CustomPreferencesIdentifier {

    companion object : CustomPreferences<AppearancePreferences> {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val APP_THEME = stringPreferencesKey("app_theme")
        private val THEME_DARK_AMOLED = booleanPreferencesKey("theme_dark_amoled")

        override fun transform(preferences: Preferences): AppearancePreferences {
            return AppearancePreferences(
                themeMode = ThemeMode.valueOf(
                    preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name
                ),
                appTheme = AppTheme.valueOf(
                    preferences[APP_THEME] ?: AppTheme.DEFAULT.name
                ),
                themeDarkAmoled = preferences[THEME_DARK_AMOLED] ?: false
            )
        }

        override fun setPrefs(newValue: AppearancePreferences, preferences: MutablePreferences) {
            preferences[THEME_MODE] = newValue.themeMode.name
            preferences[APP_THEME] = newValue.appTheme.name
            preferences[THEME_DARK_AMOLED] = newValue.themeDarkAmoled
        }
    }
}



