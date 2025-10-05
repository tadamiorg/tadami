package com.sf.tadami.ui.utils

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.sf.tadami.ui.themes.AppTheme
import com.sf.tadami.ui.themes.TadamiTheme

inline fun ComposeView.setComposeContent(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    isDark: Boolean? = null, crossinline content: @Composable () -> Unit
) {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        TadamiTheme(appTheme = appTheme, amoled = amoled, isDark = isDark) {
            content()
        }
    }
}

inline fun ComponentActivity.setComposeContent(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    isDark: Boolean? = null,
    parent: CompositionContext? = null,
    crossinline content: @Composable () -> Unit,
) {
    setContent(parent) {
        TadamiTheme(appTheme = appTheme, amoled = amoled, isDark = isDark) {
            content()
        }
    }
}