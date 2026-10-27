package com.sf.tadami.ui.main.onboarding.steps

import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.Injekt
import com.sf.tadami.ui.PlatformUi
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sf.tadami.preferences.appearance.AppearancePreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.more.settings.widget.AppThemeModePreference
import com.sf.tadami.ui.tabs.more.settings.widget.AppThemePreference

public class ThemeStep : OnboardingStep {

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
                    Injekt.get<PlatformUi>().applyThemeMode(it)
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
