package com.sf.tadami.ui.components.modal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

public val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** Fade + scale from [initialScale], the centred dialogs. */
public fun dialogEnter(initialScale: Float): EnterTransition =
    fadeIn(tween(220, easing = EmphasizedDecelerate)) +
        scaleIn(tween(220, easing = EmphasizedDecelerate), initialScale = initialScale)

/** Slide in from the end, the player side panel. */
public fun slideFromEndEnter(): EnterTransition =
    fadeIn(tween(250)) + slideInHorizontally(tween(250, easing = EmphasizedDecelerate)) { it / 2 }

/**
 * A window covering the screen, edge to edge: the platform dim is the scrim ([scrimAlpha] of black),
 * [content] sits centred in the safe drawing area (the keyboard included) minus the window margin. A
 * tap outside the content closes it when [dismissOnClickOutside].
 */
@Composable
fun ModalWindow(
    onDismissRequest: () -> Unit,
    scrimAlpha: Float,
    modifier: Modifier = Modifier,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    enter: EnterTransition = dialogEnter(0.95f),
    content: @Composable () -> Unit,
) {
    val outside by rememberUpdatedState(dismissOnClickOutside)
    val dismiss by rememberUpdatedState(onDismissRequest)
    FullWindowDialog(
        onDismissRequest = onDismissRequest,
        scrimAlpha = scrimAlpha,
        dismissOnBackPress = dismissOnBackPress,
    ) {
        val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
        // Where the content is, in the window: a tap there isn't outside. The content's own taps are
        // left alone, a text field needs them to take the focus.
        var contentBounds by remember { mutableStateOf<Rect?>(null) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (outside && contentBounds?.contains(offset) != true) dismiss()
                    }
                }
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(ModalDefaults.WindowMargin),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(visibleState = visibleState, enter = enter) {
                Box(modifier = modifier.onGloballyPositioned { contentBounds = it.boundsInWindow() }) {
                    content()
                }
            }
        }
    }
}
