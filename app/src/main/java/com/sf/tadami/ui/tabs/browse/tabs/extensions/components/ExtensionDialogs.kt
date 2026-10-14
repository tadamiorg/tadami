package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton
import com.sf.tadami.ui.utils.padding

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

/** Before trusting the signing key of an untrusted extension, and so every extension signed with it. */
@Composable
fun TrustExtensionDialog(
    extension: Extension.Installed,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    CustomAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(R.string.ext_trust_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                Column {
                    Text(text = extension.name, style = MaterialTheme.typography.titleSmall)
                    Text(text = extension.pkgName, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text(text = stringResource(R.string.ext_trust_fingerprint), style = MaterialTheme.typography.labelMedium)
                    SelectionContainer {
                        Text(
                            text = extension.signatureHash?.let(::groupedFingerprint) ?: "?",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(text = stringResource(R.string.ext_trust_warning))
            }
        },
        confirmButton = {
            DefaultDialogConfirmButton(text = R.string.ext_trust, enabled = extension.signatureHash != null) {
                onConfirm()
                onDismissRequest()
            }
        },
        dismissButton = { DefaultDialogCancelButton() },
    )
}

/** [hex], a SHA-256 fingerprint, uppercase in groups of 4 characters. */
internal fun groupedFingerprint(hex: String): String = hex.uppercase().chunked(4).joinToString(" ")

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
