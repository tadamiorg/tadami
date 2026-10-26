package com.sf.tadami.navigation.animations

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally


fun slideOutHorizontallyAndFadeOut(forward : Boolean = false): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { fullWidth -> if (forward) (fullWidth * 0.05).toInt() else -(fullWidth * 0.05).toInt() },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    ) + fadeOut()
}