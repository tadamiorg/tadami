package com.sf.tadami.navigation.animations

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut

fun fadeOut(): ExitTransition {
    return  fadeOut(
        targetAlpha = 0f,
        animationSpec = tween(durationMillis = 195, easing = FastOutSlowInEasing)
    )
}