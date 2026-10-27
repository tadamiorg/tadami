package com.sf.tadami.ui.themes

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
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
    val dark = isDark ?: when (uiPreferences.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark()
    }
    ProvideDarkMode(dark) {
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
