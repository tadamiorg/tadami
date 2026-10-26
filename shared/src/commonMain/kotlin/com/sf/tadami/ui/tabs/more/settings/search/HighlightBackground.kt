package com.sf.tadami.ui.tabs.more.settings.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/** Blinks the background of the setting a search result opened, when it first shows */
@Composable
fun Modifier.highlightBackground(highlighted: Boolean): Modifier {
    var highlight by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (highlighted) {
            highlight = true
            delay(3.seconds)
            highlight = false
        }
    }
    val color by animateColorAsState(
        targetValue = if (highlight) MaterialTheme.colorScheme.surfaceTint.copy(alpha = .12f) else Color.Transparent,
        animationSpec = if (highlight) {
            repeatable(
                iterations = 5,
                animation = tween(durationMillis = 200),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(offsetMillis = 600),
            )
        } else {
            tween(durationMillis = 200)
        },
        label = "highlight",
    )
    return this.background(color)
}
