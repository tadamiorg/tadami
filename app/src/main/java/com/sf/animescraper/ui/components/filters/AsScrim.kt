package com.sf.animescraper.ui.components.filters

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sf.animescraper.ui.utils.clickableNoIndication

@Composable
fun AsScrim(
    color : Color = MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
    visible : Boolean = false,
    onClicked : () -> Unit = {}
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = TweenSpec()
    )

    val modifiers = if(visible) Modifier.clickableNoIndication { onClicked() } else Modifier

    Canvas(
        Modifier
            .fillMaxSize()
            .then(modifiers)
    ) {
        drawRect(color = color, alpha = alpha)
    }
}