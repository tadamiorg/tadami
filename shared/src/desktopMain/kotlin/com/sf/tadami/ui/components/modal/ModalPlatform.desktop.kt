package com.sf.tadami.ui.components.modal

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
actual fun screenBarsAppearance(): BarsAppearance {
    val lightTheme = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    return BarsAppearance(lightTheme, lightTheme)
}

@Composable
actual fun TransparentDialogBars() = Unit

actual fun modalSheetProperties(bars: BarsAppearance): ModalBottomSheetProperties = ModalBottomSheetProperties()

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
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            scrimColor = Color.Black.copy(alpha = scrimAlpha),
        ),
        content = content,
    )
}
