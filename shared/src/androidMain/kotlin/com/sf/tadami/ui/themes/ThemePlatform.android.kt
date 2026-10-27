package com.sf.tadami.ui.themes

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

// The activity keeps the configuration of the mode set when it started, the system has the real one
@Composable
actual fun isSystemDark(): Boolean = Resources.getSystem().configuration.isNight()

/** isSystemInDarkTheme() reads the configuration. */
@Composable
actual fun ProvideDarkMode(dark: Boolean, content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
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
    CompositionLocalProvider(LocalConfiguration provides themeConfiguration, content = content)
}

private fun Configuration.isNight(): Boolean =
    uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
