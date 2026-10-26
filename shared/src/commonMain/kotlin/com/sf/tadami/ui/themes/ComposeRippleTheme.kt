package com.sf.tadami.ui.themes

import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RippleConfiguration
import androidx.compose.ui.graphics.Color

private val ComposeRippleAlpha = RippleAlpha(
    pressedAlpha = 0.2f,
    focusedAlpha = 0.4f,
    draggedAlpha = 0.4f,
    hoveredAlpha = 0.4f
)

@OptIn(ExperimentalMaterial3Api::class)
val ComposeRippleConfiguration = RippleConfiguration(color = Color.Red, rippleAlpha = ComposeRippleAlpha)