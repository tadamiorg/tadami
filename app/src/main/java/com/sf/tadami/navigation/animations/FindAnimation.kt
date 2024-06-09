package com.sf.tadami.navigation.animations

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavBackStackEntry
import com.sf.tadami.navigation.graphs.home.HomeNavItems

fun AnimatedContentTransitionScope<NavBackStackEntry>.findExitAnimation(forward : Boolean) : ExitTransition {
    return when {
        !HomeNavItems.includes(initialState.destination.route) || !HomeNavItems.includes(targetState.destination.route) -> slideOutHorizontallyAndFadeOut(forward)
        else -> fadeOut()
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.findEnterAnimation(forward : Boolean) : EnterTransition {
    return when {
        !HomeNavItems.includes(initialState.destination.route) || !HomeNavItems.includes(targetState.destination.route) -> slideInHorizontallyAndFadeIn(forward)
        else -> fadeIn()
    }
}