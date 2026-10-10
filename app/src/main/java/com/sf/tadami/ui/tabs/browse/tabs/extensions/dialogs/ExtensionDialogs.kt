package com.sf.tadami.ui.tabs.browse.tabs.extensions.dialogs

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.ui.components.dialog.alert.CustomAlertDialog
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogCancelButton
import com.sf.tadami.ui.components.dialog.alert.DefaultDialogConfirmButton

/**
 * What the Extensions tab is currently asking the user to confirm.
 *
 * Installing and removing an extension both used to be fronted by a system dialog. Neither goes
 * through the OS any more, so without these the taps would be silently destructive.
 */
sealed interface ExtensionDialog {
    data class Install(val extension: Extension.Available) : ExtensionDialog
    data class Uninstall(val name: String, val pkgName: String) : ExtensionDialog

    /** Offer to remove every installed extension package that has already been adopted. */
    data class RemoveLegacyPackages(val count: Int) : ExtensionDialog
}

/**
 * A dialog description with the extension name picked out.
 *
 * The name is the one part of the sentence worth checking before confirming something
 * irreversible, so it shouldn't carry the same weight as the rest of it.
 */
@Composable
private fun DialogDescription(@StringRes template: Int, name: String) {
    val raw = stringResource(template)
    val description = remember(raw, name) {
        val (before, after) = raw.split(NAME_PLACEHOLDER, limit = 2)
            .let { it.first() to it.getOrElse(1) { "" } }
        buildAnnotatedString {
            append(before)
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(name) }
            append(after)
        }
    }
    Text(text = description, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun DialogDescription(@PluralsRes template: Int, count: Int) {
    Text(
        text = pluralStringResource(template, count, count),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun InstallExtensionDialog(
    extensionName: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    CustomAlertDialog(
        title = { Text(text = stringResource(R.string.ext_install_confirm_title)) },
        text = { DialogDescription(R.string.ext_install_confirm_text, extensionName) },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            DefaultDialogConfirmButton(text = R.string.ext_install_confirm_action) {
                onConfirm()
                onDismissRequest()
            }
        },
        dismissButton = { DefaultDialogCancelButton() },
    )
}

@Composable
fun UninstallExtensionDialog(
    extensionName: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    CustomAlertDialog(
        title = { Text(text = stringResource(R.string.ext_uninstall_confirm_title)) },
        text = { DialogDescription(R.string.ext_uninstall_confirm_text, extensionName) },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            DefaultDialogConfirmButton(text = R.string.ext_uninstall) {
                onConfirm()
                onDismissRequest()
            }
        },
        dismissButton = { DefaultDialogCancelButton() },
    )
}

/**
 * Confirms before handing off to Android's uninstall screen, once per leftover package.
 *
 * Worth asking first: the system dialog alone gives no hint why Tadami is suddenly removing apps
 * the user installed deliberately, and there will be one of them per extension.
 */
@Composable
fun RemoveLegacyPackagesDialog(
    count: Int,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    CustomAlertDialog(
        title = { Text(text = stringResource(R.string.ext_legacy_cleanup_title)) },
        text = { DialogDescription(R.plurals.ext_legacy_cleanup_text, count) },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            DefaultDialogConfirmButton(text = R.string.ext_legacy_cleanup_action) {
                onConfirm()
                onDismissRequest()
            }
        },
        dismissButton = { DefaultDialogCancelButton() },
    )
}

private const val NAME_PLACEHOLDER = "%1\$s"
