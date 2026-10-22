package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.api.ExtensionRepos
import com.sf.tadami.extension.model.SourceIdConflict
import com.sf.tadami.ui.components.modal.ModalAction
import com.sf.tadami.ui.components.modal.ModalBodyText
import com.sf.tadami.ui.components.modal.ModalConsequence
import com.sf.tadami.ui.components.modal.ModalSubject
import com.sf.tadami.ui.components.modal.TadaDestructiveDialog
import com.sf.tadami.ui.components.modal.TadaDialog
import com.sf.tadami.ui.utils.padding

@Composable
fun UninstallExtensionDialog(
    extension: Extension.Installed,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    TadaDestructiveDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.ext_uninstall_title),
        subject = extensionSubject(extension),
        message = stringResource(R.string.ext_uninstall_message),
        confirmLabel = stringResource(R.string.ext_uninstall),
        onConfirm = {
            onConfirm()
            onDismissRequest()
        },
    )
}

/**
 * Before installing a new extension, as nothing asks the system for it.
 *
 * @param sourceIdConflict why it wouldn't be usable once installed, its source IDs being another
 * extension's, null when it would be
 */
@Composable
fun InstallExtensionDialog(
    extension: Extension.Available,
    sourceIdConflict: SourceIdConflict?,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    TadaDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.ext_install_title),
        subject = extensionSubject(extension),
        confirm = ModalAction(stringResource(R.string.ext_install)) {
            onConfirm()
            onDismissRequest()
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
            val repoName = ExtensionRepos.safeName(LocalExtensionRepos.current.metas[extension.repoUrl]?.name)
                ?.let { "$it (${ExtensionRepos.name(extension.repoUrl)})" }
                ?: ExtensionRepos.name(extension.repoUrl)
            Text(
                text = stringResource(
                    if (ExtensionRepos.isOfficial(extension.repoUrl)) R.string.ext_install_repo_official else R.string.ext_install_repo,
                    repoName,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ModalBodyText(stringResource(R.string.ext_install_message))
            val conflictMessage = when (sourceIdConflict) {
                SourceIdConflict.Duplicate -> stringResource(R.string.ext_issue_source_id_duplicate_message)
                SourceIdConflict.Reserved -> stringResource(R.string.ext_install_source_id_reserved)
                is SourceIdConflict.TakenBy -> stringResource(R.string.ext_install_source_id_conflict, sourceIdConflict.ownerName)
                null -> null
            }
            conflictMessage?.let { ModalConsequence(it) }
        }
    }
}

/** The icon, name, version and language of [extension], in the header of a dialog about it. */
@Composable
private fun extensionSubject(extension: Extension) = ModalSubject(
    name = extension.name,
    meta = stringResource(
        R.string.ext_install_version_lang,
        extension.versionName,
        stringResource(extension.lang?.getRes() ?: R.string.language_unknown),
    ),
    icon = { ExtensionIcon(extension = extension, modifier = it) },
)

/** Before trusting the signing key of an untrusted extension, and so every extension signed with it. */
@Composable
fun TrustExtensionDialog(
    extension: Extension.Installed,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    TadaDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.ext_trust_title),
        icon = Icons.Outlined.Security,
        confirm = ModalAction(stringResource(R.string.ext_trust), enabled = extension.signatureHash != null) {
            onConfirm()
            onDismissRequest()
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
            Column {
                Text(text = extension.name, style = MaterialTheme.typography.titleSmall)
                Text(text = extension.pkgName, style = MaterialTheme.typography.bodySmall)
            }
            Column {
                Text(text = stringResource(R.string.ext_trust_fingerprint), style = MaterialTheme.typography.labelMedium)
                Fingerprint(extension.signatureHash?.let(::groupedFingerprint) ?: "?")
            }
            ModalBodyText(stringResource(R.string.ext_trust_warning))
        }
    }
}

/** A key fingerprint, selectable, in a monospace box. */
@Composable
internal fun Fingerprint(text: String) {
    SelectionContainer {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(8.dp),
        )
    }
}

/** [hex], a SHA-256 fingerprint, uppercase in groups of 4 characters. */
internal fun groupedFingerprint(hex: String): String = hex.uppercase().chunked(4).joinToString(" ")

/** Before the uninstall dialogs of the system, one per extension app. */
@Composable
fun RemoveLegacyPackagesDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    TadaDestructiveDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.ext_legacy_packages_title),
        message = stringResource(R.string.ext_legacy_packages_message),
        confirmLabel = stringResource(R.string.ext_uninstall),
        onConfirm = {
            onConfirm()
            onDismissRequest()
        },
    )
}
