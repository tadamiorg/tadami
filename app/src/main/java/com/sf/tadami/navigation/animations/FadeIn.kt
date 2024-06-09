package com.sf.tadami.navigation.animations

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn

fun fadeIn(): EnterTransition {
    return  fadeIn(
        initialAlpha = 0f,
        animationSpec = tween(durationMillis = 195, easing = FastOutSlowInEasing)
    )
}