package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton

@Composable
fun UninstallExtensionDialog(
    extensionName: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    CustomAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.ext_uninstall)) },
        text = { Text(text = stringResource(R.string.ext_uninstall_confirm, extensionName)) },
        confirmButton = {
            DefaultDialogConfirmButton(text = R.string.ext_uninstall) {
                onConfirm()
                onDismissRequest()
            }
        },
        dismissButton = { DefaultDialogCancelButton() },
    )
}

/** Before the uninstall dialogs of the system, one per extension app. */
@Composable
fun RemoveLegacyPackagesDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    CustomAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.ext_legacy_packages_title)) },
        text = { Text(text = stringResource(R.string.ext_legacy_packages_message)) },
        confirmButton = {
            DefaultDialogConfirmButton(text = R.string.ext_uninstall) {
                onConfirm()
                onDismissRequest()
            }
        },
        dismissButton = { DefaultDialogCancelButton() },
    )
}
