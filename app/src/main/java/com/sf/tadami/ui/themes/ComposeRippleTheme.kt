package com.sf.tadami.ui.themes

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ComposeRippleTheme : RippleTheme {

    @Composable
    override fun defaultColor(): Color {
        return MaterialTheme.colorScheme.primary;
    }

    @Composable
    override fun rippleAlpha(): RippleAlpha {
        return RippleTheme.defaultRippleAlpha(
            MaterialTheme.colorScheme.background,
            lightTheme = true
        )
    }
}