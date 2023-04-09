package com.sf.tadami.ui.components.dialog.simple

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.ButtonsCrossAxisSpacing
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.ButtonsMainAxisSpacing
import com.sf.tadami.ui.components.dialog.alert.AlertDialogContent
import com.sf.tadami.ui.components.dialog.alert.AlertDialogFlowRow

@Composable
fun SimpleDialog(
    modifier: Modifier = Modifier,
    opened : Boolean,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.inverseSurface,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    onDismissRequest: () -> Unit,
    text: @Composable (() -> Unit)? = null,
) {
    Crossfade(modifier = Modifier.zIndex(5f),targetState = opened) { visible ->
        if (visible) {
            Box(
                contentAlignment = Alignment.Center
            )
            {
                Surface(modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        onDismissRequest()
                    },
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                ) {}
                AlertDialogContent(
                    modifier = modifier.wrapContentWidth().aspectRatio(1f),
                    buttons = {
                        AlertDialogFlowRow(
                            mainAxisSpacing = ButtonsMainAxisSpacing,
                            crossAxisSpacing = ButtonsCrossAxisSpacing
                        ) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    },
                    title = title,
                    text = text,
                    containerColor = containerColor,
                    buttonContentColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = titleContentColor,
                    textContentColor = textContentColor,
                )
            }
        }
    }
}