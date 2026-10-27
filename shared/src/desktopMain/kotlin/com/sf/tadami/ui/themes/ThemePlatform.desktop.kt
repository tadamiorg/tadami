package com.sf.tadami.ui.themes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable
actual fun isSystemDark(): Boolean = isSystemInDarkTheme()

@Composable
actual fun ProvideDarkMode(dark: Boolean, content: @Composable () -> Unit) = content()
