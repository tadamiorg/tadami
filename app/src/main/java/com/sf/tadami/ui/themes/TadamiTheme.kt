package com.sf.tadami.ui.themes

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.R
import com.sf.tadami.preferences.appearance.AppearancePreferences
import com.sf.tadami.preferences.appearance.ThemeMode
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.themes.colorschemes.DoomColorScheme
import com.sf.tadami.ui.themes.colorschemes.GreenAppleColorScheme
import com.sf.tadami.ui.themes.colorschemes.LavenderColorScheme
import com.sf.tadami.ui.themes.colorschemes.MidnightDuskColorScheme
import com.sf.tadami.ui.themes.colorschemes.StrawberryColorScheme
import com.sf.tadami.ui.themes.colorschemes.TadamiColorScheme
import com.sf.tadami.ui.themes.colorschemes.TakoColorScheme
import com.sf.tadami.ui.themes.colorschemes.TealTurqoiseColorScheme
import com.sf.tadami.ui.themes.colorschemes.TidalWaveColorScheme
import com.sf.tadami.ui.themes.colorschemes.YinYangColorScheme
import com.sf.tadami.ui.themes.colorschemes.YotsubaColorScheme
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Follows the appearance settings as they change, without recreating the activity: recreating it drew
 * a first frame of the old screen state, and the screens jumped.
 */
@Composable
fun TadamiTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    isDark: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val uiPreferences by rememberDataStoreState(AppearancePreferences).value.collectAsState()
    val configuration = LocalConfiguration.current
    val dark = isDark ?: when (uiPreferences.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        // The activity keeps the configuration of the mode set when it started, the system has the real one
        ThemeMode.SYSTEM -> Resources.getSystem().configuration.isNight()
    }
    // isSystemInDarkTheme() reads it, below too
    val themeConfiguration = remember(configuration, dark) {
        if (configuration.isNight() == dark) {
            configuration
        } else {
            Configuration(configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
        }
    }
    CompositionLocalProvider(LocalConfiguration provides themeConfiguration) {
        MaterialTheme(
            colorScheme = getThemeColorScheme(
                appTheme ?: uiPreferences.appTheme,
                dark,
                amoled ?: uiPreferences.themeDarkAmoled,
            ),
            content = content
        )
    }
}

private fun Configuration.isNight(): Boolean =
    uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

private fun getThemeColorScheme(
    appTheme: AppTheme,
    isDark : Boolean,
    amoled: Boolean,
): ColorScheme {
    val colorScheme = when (appTheme) {
        AppTheme.DEFAULT -> TadamiColorScheme
        AppTheme.GREEN_APPLE -> GreenAppleColorScheme
        AppTheme.LAVENDER -> LavenderColorScheme
        AppTheme.YOTSUBA -> YotsubaColorScheme
        AppTheme.YINYANG -> YinYangColorScheme
        AppTheme.TEALTURQUOISE -> TealTurqoiseColorScheme
        AppTheme.TIDAL_WAVE -> TidalWaveColorScheme
        AppTheme.TAKO -> TakoColorScheme
        AppTheme.STRAWBERRY_DAIQUIRI -> StrawberryColorScheme
        AppTheme.MIDNIGHT_DUSK -> MidnightDuskColorScheme
        AppTheme.DOOM -> DoomColorScheme
        else -> TadamiColorScheme
    }
    return colorScheme.getColorScheme(isDark, amoled)
}

fun getNotificationsColor(context: Context) : Int{
    val dataStore: DataStore<Preferences> = Injekt.get()
    val appearancePreferences: AppearancePreferences = runBlocking {
        dataStore.getPreferencesGroup(AppearancePreferences)
    }
    val resource = when (appearancePreferences.appTheme) {
        AppTheme.LAVENDER -> R.color.lavender_primary
        AppTheme.GREEN_APPLE -> R.color.green_apple_primary
        AppTheme.TAKO -> R.color.tako_primary
        AppTheme.TIDAL_WAVE -> R.color.tidal_wave_primary
        AppTheme.YINYANG -> R.color.yin_yang_primary
        AppTheme.STRAWBERRY_DAIQUIRI -> R.color.strawberry_primary
        AppTheme.YOTSUBA -> R.color.yotsuba_primary
        AppTheme.TEALTURQUOISE -> R.color.teal_turquoise_primary
        AppTheme.MIDNIGHT_DUSK -> R.color.midnight_primary
        AppTheme.DOOM -> R.color.doom_primary
        AppTheme.DEFAULT -> R.color.tadami_primary

    }
    return context.getColor(resource)
}