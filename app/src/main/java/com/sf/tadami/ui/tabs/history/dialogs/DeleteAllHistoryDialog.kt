package com.sf.tadami.ui.tabs.history.dialogs

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.ui.components.modal.TadaDestructiveDialog

@Composable
fun DeleteAllHistoryDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    TadaDestructiveDialog(
        title = stringResource(Res.string.action_remove_everything),
        message = stringResource(Res.string.clear_history_confirmation),
        confirmLabel = stringResource(Res.string.ok),
        onConfirm = {
            onConfirm()
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
    )
}
