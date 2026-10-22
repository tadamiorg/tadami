package com.sf.tadami.ui.tabs.history.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.ui.components.modal.DialogOption
import com.sf.tadami.ui.components.modal.TadaDestructiveDialog

@Composable
fun DeleteHistoryDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var removeEverything by rememberSaveable { mutableStateOf(false) }

    TadaDestructiveDialog(
        title = stringResource(id = R.string.action_delete),
        message = stringResource(R.string.dialog_with_checkbox_remove_description),
        option = DialogOption(
            label = stringResource(R.string.dialog_with_checkbox_reset),
            checked = removeEverything,
            onCheckedChange = { removeEverything = it },
        ),
        confirmLabel = stringResource(R.string.action_delete),
        onConfirm = {
            onConfirm(removeEverything)
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
    )
}
