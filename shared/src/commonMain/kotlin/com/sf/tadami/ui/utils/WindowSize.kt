package com.sf.tadami.ui.utils

import androidx.compose.runtime.Composable

/** Whether the screen is wide enough for the tablet layouts. */
@Composable
expect fun isTabletUi(): Boolean

/** Whether the screen is in landscape, wider than tall. */
@Composable
expect fun isLandscape(): Boolean
