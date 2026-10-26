package com.sf.tadami.navigation.animations

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally

fun slideInHorizontallyAndFadeIn(forward : Boolean = true): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { fullWidth -> if (forward) (fullWidth * 0.05).toInt() else -(fullWidth * 0.05).toInt() },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    ) + fadeIn()
}