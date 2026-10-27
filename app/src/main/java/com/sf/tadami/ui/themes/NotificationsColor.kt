package com.sf.tadami.ui.themes

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.R
import com.sf.tadami.preferences.appearance.AppearancePreferences
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

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