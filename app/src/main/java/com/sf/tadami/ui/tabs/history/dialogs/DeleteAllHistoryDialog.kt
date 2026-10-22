package com.sf.tadami.ui.tabs.history.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.ui.components.modal.TadaDestructiveDialog

@Composable
fun DeleteAllHistoryDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    TadaDestructiveDialog(
        title = stringResource(R.string.action_remove_everything),
        message = stringResource(R.string.clear_history_confirmation),
        confirmLabel = stringResource(android.R.string.ok),
        onConfirm = {
            onConfirm()
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
    )
}
