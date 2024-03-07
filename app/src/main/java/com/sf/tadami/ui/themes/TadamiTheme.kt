package com.sf.tadami.ui.themes

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import com.sf.tadami.R
import com.sf.tadami.preferences.appearance.AppearancePreferences
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


@Composable
fun TadamiTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    isDark: Boolean? = null,
    content: @Composable () -> Unit,
) {
    MaterialTheme (
        colorScheme = getThemeColorScheme(appTheme, isDark,amoled),
        content = content
    )
}


@Composable
@ReadOnlyComposable
private fun getThemeColorScheme(
    appTheme: AppTheme?,
    isDark : Boolean?,
    amoled: Boolean?,
): ColorScheme {
    val dataStore: DataStore<Preferences> = Injekt.get()
    val uiPreferences = runBlocking {
        dataStore.getPreferencesGroup(AppearancePreferences)
    }
    val colorScheme = when (appTheme ?: uiPreferences.appTheme) {
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
        else -> TadamiColorScheme
    }
    return colorScheme.getColorScheme(
        isDark ?: isSystemInDarkTheme(),
        amoled ?: uiPreferences.themeDarkAmoled,
    )
}

fun getNotificationsColor(context: Context) : Int{
    val dataStore: DataStore<Preferences> = Injekt.get()
    val appearancePreferences: AppearancePreferences = runBlocking {
        dataStore.getPreferencesGroup(AppearancePreferences)
    }
    val resource = when (appearancePreferences.appTheme) {
        AppTheme.LAVENDER -> R.color.lavender_primary
        AppTheme.GREEN_APPLE -> R.color.greenapple_primary
        AppTheme.TAKO -> R.color.tako_primary
        AppTheme.TIDAL_WAVE -> R.color.tidalwave_primary
        AppTheme.YINYANG -> R.color.yinyang_primary
        AppTheme.STRAWBERRY_DAIQUIRI -> R.color.strawberry_primary
        AppTheme.YOTSUBA -> R.color.yotsuba_primary
        AppTheme.TEALTURQUOISE -> R.color.tealturquoise_primary
        AppTheme.MIDNIGHT_DUSK -> R.color.midnightdusk_primary
        AppTheme.DEFAULT -> R.color.base_primary

    }
    return context.getColor(resource)
}