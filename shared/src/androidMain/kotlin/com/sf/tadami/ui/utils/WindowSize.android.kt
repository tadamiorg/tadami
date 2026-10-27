package com.sf.tadami.ui.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

private const val TABLET_UI_REQUIRED_SCREEN_WIDTH_DP = 720

fun Configuration.isTabletUi(): Boolean {
    return smallestScreenWidthDp >= TABLET_UI_REQUIRED_SCREEN_WIDTH_DP
}

@Composable
@ReadOnlyComposable
actual fun isTabletUi(): Boolean {
    return LocalConfiguration.current.isTabletUi()
}

@Composable
@ReadOnlyComposable
actual fun isLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
