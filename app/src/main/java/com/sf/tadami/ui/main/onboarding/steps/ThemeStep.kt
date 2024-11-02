package com.sf.tadami.ui.main.onboarding.steps

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sf.tadami.preferences.appearance.AppearancePreferences
import com.sf.tadami.preferences.appearance.setAppCompatDelegateThemeMode
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.more.settings.widget.AppThemeModePreference
import com.sf.tadami.ui.tabs.more.settings.widget.AppThemePreference

internal class ThemeStep : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val uiPreferencesState = rememberDataStoreState(AppearancePreferences)
        val uiPreferences by uiPreferencesState.value.collectAsState()

        Column {
            AppThemeModePreference(
                value = uiPreferences.themeMode,
                onItemClick = {
                    uiPreferencesState.setValue(uiPreferences.copy(themeMode = it))
                    setAppCompatDelegateThemeMode(it)
                },
            )

            AppThemePreference(
                value = uiPreferences.appTheme,
                amoled = uiPreferences.themeDarkAmoled,
                onItemClick = { uiPreferencesState.setValue(uiPreferences.copy(appTheme = it)) },
            )
        }
    }
}
