package com.sf.tadami.ui.components.modal

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun FullWindowDialog(
    onDismissRequest: () -> Unit,
    scrimAlpha: Float,
    dismissOnBackPress: Boolean,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            // Handled by the content, the window covers the whole screen
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
        content()
    }
}

actual fun modalSheetProperties(bars: BarsAppearance): ModalBottomSheetProperties = ModalBottomSheetProperties(
    isAppearanceLightStatusBars = bars.lightStatusBars,
    isAppearanceLightNavigationBars = bars.lightNavigationBars,
)

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

@Composable
actual fun screenBarsAppearance(): BarsAppearance {
    val view = LocalView.current
    val lightTheme = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val activityWindow = view.context.findActivity()?.window
        ?: return BarsAppearance(lightTheme, lightTheme)
    val controller = WindowCompat.getInsetsController(activityWindow, activityWindow.decorView)
    return BarsAppearance(controller.isAppearanceLightStatusBars, controller.isAppearanceLightNavigationBars)
}

/**
 * Otherwise the system paints its own background behind the bars of the focused window, which changes
 * their colour while it's open and blinks when it closes.
 */
@Composable
actual fun TransparentDialogBars() {
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
        val screenInsets = activityWindow?.decorView?.let { ViewCompat.getRootWindowInsets(it) }
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = screen?.isAppearanceLightStatusBars ?: lightTheme
            isAppearanceLightNavigationBars = screen?.isAppearanceLightNavigationBars ?: lightTheme
            // Hidden bars (the player is immersive) stay hidden: they would come back and move the screen
            val hidden = listOf(WindowInsetsCompat.Type.statusBars(), WindowInsetsCompat.Type.navigationBars())
                .filter { screenInsets != null && !screenInsets.isVisible(it) }
            if (hidden.isNotEmpty()) {
                systemBarsBehavior = screen?.systemBarsBehavior ?: WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hidden.forEach { hide(it) }
            }
        }
    }
}
