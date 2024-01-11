package com.sf.tadami.ui.tabs.settings.screens.appearance

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.ui.tabs.settings.components.PreferenceScreen
import com.sf.tadami.ui.tabs.settings.model.DataStoreState
import com.sf.tadami.ui.tabs.settings.model.Preference
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.settings.widget.AppThemeModePreference
import com.sf.tadami.ui.tabs.settings.widget.AppThemePreference

class AppearancePreferencesScreen(
    navController: NavHostController,
) : PreferenceScreen {

    override val title: Int = R.string.label_appearance

    override val backHandler: (() -> Unit) = {
        navController.navigateUp()
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val appearancePreferencesState = rememberDataStoreState(AppearancePreferences)
        val appearancePreferences by appearancePreferencesState.value.collectAsState()

        return listOf(
            getThemeGroup(prefState = appearancePreferencesState, prefs = appearancePreferences)
        )
    }

    @Composable
    fun getThemeGroup(
        prefState: DataStoreState<AppearancePreferences>,
        prefs: AppearancePreferences
    ): Preference.PreferenceCategory {
        val context = LocalContext.current
        return Preference.PreferenceCategory(
            title = stringResource(id = R.string.label_theme),
            preferenceItems = listOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(id = R.string.label_theme)
                ){
                    Column {
                        AppThemeModePreference(
                            value = prefs.themeMode,
                            onItemClick = {
                                prefState.setValue(prefs.copy(themeMode = it))
                                setAppCompatDelegateThemeMode(it)
                            },
                        )

                        AppThemePreference(
                            value = prefs.appTheme,
                            amoled = prefs.themeDarkAmoled,
                            onItemClick = { prefState.setValue(prefs.copy(appTheme = it)) },
                        )
                    }
                },
                Preference.PreferenceItem.TogglePreference(
                    title = stringResource(id = R.string.dark_theme_pure_black),
                    value = prefs.themeDarkAmoled,
                    enabled = isDarkMode(context,prefs),
                    onValueChanged = { themeDarkAmoled ->
                        (context as? Activity)?.let { ActivityCompat.recreate(it) }
                        prefState.setValue(prefs.copy(themeDarkAmoled = themeDarkAmoled))
                        true
                    }
                ),
            )
        )
    }
}

private fun isDarkMode(context : Context,prefs : AppearancePreferences) : Boolean{
    val currentNightMode: Int = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return when{
        prefs.themeMode == ThemeMode.DARK -> true
        prefs.themeMode == ThemeMode.SYSTEM && currentNightMode == Configuration.UI_MODE_NIGHT_YES -> true
        else -> false
    }
}