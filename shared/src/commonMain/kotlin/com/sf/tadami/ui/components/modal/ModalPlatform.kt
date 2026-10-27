package com.sf.tadami.ui.components.modal

import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.Composable

/** The icons of the screen's bars: light status bar, light navigation bar (dark icons). */
data class BarsAppearance(val lightStatusBars: Boolean, val lightNavigationBars: Boolean)

/** How the screen under a dialog or sheet draws the icons of its bars, the theme's if it can't be read. */
@Composable
expect fun screenBarsAppearance(): BarsAppearance

/**
 * In a dialog or sheet window: transparent system bars drawn by the window itself, without the
 * contrast scrim, their icons as the screen under it has them. Nothing where there are no system bars.
 */
@Composable
expect fun TransparentDialogBars()

/** The properties of a modal bottom sheet whose bars keep the icons of [bars]. */
expect fun modalSheetProperties(bars: BarsAppearance): ModalBottomSheetProperties

/**
 * A dialog covering the whole window, edge to edge, dimmed by [scrimAlpha] of black, see [ModalWindow].
 */
@Composable
expect fun FullWindowDialog(
    onDismissRequest: () -> Unit,
    scrimAlpha: Float,
    dismissOnBackPress: Boolean,
    content: @Composable () -> Unit,
)
