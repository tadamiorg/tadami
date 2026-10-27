package com.sf.tadami.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

private val TABLET_UI_REQUIRED_WIDTH = 720.dp

/** The window, which the user resizes, rather than the screen. */
@Composable
actual fun isTabletUi(): Boolean = LocalWindowInfo.current.containerDpSize.width >= TABLET_UI_REQUIRED_WIDTH

@Composable
actual fun isLandscape(): Boolean = LocalWindowInfo.current.containerSize.let { it.width > it.height }
