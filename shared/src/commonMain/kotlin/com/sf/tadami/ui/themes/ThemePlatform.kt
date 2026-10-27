package com.sf.tadami.ui.themes

import androidx.compose.runtime.Composable

/** Whether the system is in dark mode, what the theme follows in [com.sf.tadami.preferences.appearance.ThemeMode.SYSTEM]. */
@Composable
expect fun isSystemDark(): Boolean

/** [content] with the platform's dark mode set to [dark], for what reads it rather than the theme. */
@Composable
expect fun ProvideDarkMode(dark: Boolean, content: @Composable () -> Unit)
