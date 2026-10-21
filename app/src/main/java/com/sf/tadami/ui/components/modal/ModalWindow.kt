package com.sf.tadami.ui.components.modal

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

internal val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** Fade + scale from [initialScale], the centred dialogs. */
internal fun dialogEnter(initialScale: Float): EnterTransition =
    fadeIn(tween(220, easing = EmphasizedDecelerate)) +
        scaleIn(tween(220, easing = EmphasizedDecelerate), initialScale = initialScale)

/** Slide in from the end, the player side panel. */
internal fun slideFromEndEnter(): EnterTransition =
    fadeIn(tween(250)) + slideInHorizontally(tween(250, easing = EmphasizedDecelerate)) { it / 2 }

/** Consumes the taps that would reach the scrim behind a container. */
internal fun Modifier.consumeTaps(): Modifier = pointerInput(Unit) { detectTapGestures { } }

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
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            // Handled below, the window covers the whole screen
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val window = LocalView.current.let { remember(it) { it.dialogWindow() } }
        SideEffect {
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window?.setDimAmount(scrimAlpha)
        }
        TransparentDialogBars()
        val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { if (outside) dismiss() } }
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(ModalDefaults.WindowMargin),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(visibleState = visibleState, enter = enter) {
                Box(modifier = modifier.consumeTaps()) {
                    content()
                }
            }
        }
    }
}

/** The window of the dialog [this] view is in. */
private fun View.dialogWindow(): Window? {
    var current: Any? = this
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = (current as? View)?.parent ?: (current as? android.view.ViewParent)?.parent
    }
    return null
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * In a dialog or sheet window: transparent system bars drawn by the window itself, without the
 * contrast scrim, their icons as the screen under it has them. Otherwise the system paints its own
 * background behind the bars of the focused window, which changes their colour while it's open and
 * blinks when it closes.
 */
@Composable
fun TransparentDialogBars() {
    val view = LocalView.current
    val window = remember(view) { view.dialogWindow() }
    val activityWindow = remember(view) { view.context.findActivity()?.window }
    val lightTheme = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    SideEffect {
        if (window == null) return@SideEffect
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        // The bars keep the icons of the screen, so nothing changes when the window comes and goes
        val screen = activityWindow?.let { WindowCompat.getInsetsController(it, it.decorView) }
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = screen?.isAppearanceLightStatusBars ?: lightTheme
            isAppearanceLightNavigationBars = screen?.isAppearanceLightNavigationBars ?: lightTheme
        }
    }
}
