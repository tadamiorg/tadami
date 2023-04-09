package com.sf.tadami.ui.components.dialog.alert


import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.ButtonsCrossAxisSpacing
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants.ButtonsMainAxisSpacing

@Composable
fun CustomAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.inverseSurface,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    properties: DialogProperties = DialogProperties(),
    text: @Composable (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        AlertDialogContent(
            buttons = {
                AlertDialogFlowRow(
                    mainAxisSpacing = ButtonsMainAxisSpacing,
                    crossAxisSpacing = ButtonsCrossAxisSpacing
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            },
            modifier = modifier,
            title = title,
            text = text,
            containerColor = containerColor,
            buttonContentColor = MaterialTheme.colorScheme.primary,
            titleContentColor = titleContentColor,
            textContentColor = textContentColor,
        )
    }
}



