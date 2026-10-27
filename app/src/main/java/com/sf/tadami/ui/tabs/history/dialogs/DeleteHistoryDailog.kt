package com.sf.tadami.ui.tabs.history.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.ui.components.modal.DialogOption
import com.sf.tadami.ui.components.modal.TadaDestructiveDialog

@Composable
fun DeleteHistoryDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var removeEverything by rememberSaveable { mutableStateOf(false) }

    TadaDestructiveDialog(
        title = stringResource(Res.string.action_delete),
        message = stringResource(Res.string.dialog_with_checkbox_remove_description),
        option = DialogOption(
            label = stringResource(Res.string.dialog_with_checkbox_reset),
            checked = removeEverything,
            onCheckedChange = { removeEverything = it },
        ),
        confirmLabel = stringResource(Res.string.action_delete),
        onConfirm = {
            onConfirm(removeEverything)
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
    )
}
