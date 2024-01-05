package com.sf.tadami.ui.tabs.history.dialogs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton

@Composable
fun DeleteAllHistoryDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    CustomAlertDialog(
        title = {
            Text(text = stringResource(R.string.action_remove_everything))
        },
        text = {
            Text(text = stringResource(R.string.clear_history_confirmation))
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            DefaultDialogConfirmButton(
                text = android.R.string.ok
            ) {
                onConfirm()
                onDismissRequest()
            }
        },
        dismissButton = {
            DefaultDialogCancelButton()
        },
    )
}