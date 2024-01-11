package com.sf.tadami.ui.tabs.history.dialogs

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton

@Composable
fun DeleteHistoryDialog(
    onDismissRequest : () -> Unit,
    onConfirm : (Boolean) -> Unit
) {
    var removeEverything by rememberSaveable { mutableStateOf(false) }

    CustomAlertDialog(
        title = {
            Text(text = stringResource(id = R.string.action_delete))
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            DefaultDialogConfirmButton(
                text = R.string.action_delete
            ) {
                onConfirm(removeEverything)
                onDismissRequest()
            }
        },
        dismissButton = {
            DefaultDialogCancelButton()
        }
    ) {
        Text(text = stringResource(R.string.dialog_with_checkbox_remove_description))
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .toggleable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    value = removeEverything,
                    onValueChange = { removeEverything = it },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = removeEverything,
                onCheckedChange = null,
            )
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = stringResource(R.string.dialog_with_checkbox_reset),
            )
        }
    }
}