package com.sf.tadami.ui.tabs.browse.tabs.extensions.components.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.extension.model.InstallStep
import com.sf.tadami.ui.utils.padding

@Composable
fun ExtensionItemActions(
    extension: Extension,
    installStep: InstallStep,
    modifier: Modifier = Modifier,
    onClickItemCancel: (Extension) -> Unit = {},
    onClickItemAction: (Extension) -> Unit = {},
    onClickItemSecondaryAction: (Extension) -> Unit = {},
) {
    val isIdle = installStep.isCompleted()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        when {
            installStep == InstallStep.Loading -> {}
            !isIdle -> {
                IconButton(onClick = { onClickItemCancel(extension) }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(Res.string.action_cancel),
                    )
                }
            }
            installStep == InstallStep.Error -> {
                IconButton(onClick = { onClickItemAction(extension) }) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(Res.string.retry),
                    )
                }
            }
            installStep == InstallStep.Idle -> {
                when (extension) {
                    is Extension.Installed -> {
                        IconButton(onClick = { onClickItemSecondaryAction(extension) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(Res.string.stub_text),
                            )
                        }

                        if (extension.loadIssue == ExtensionLoadIssue.UNTRUSTED) {
                            TextButton(onClick = { onClickItemAction(extension) }) {
                                Text(text = stringResource(Res.string.ext_trust))
                            }
                        } else if (extension.hasUpdate) {
                            IconButton(onClick = { onClickItemAction(extension) }) {
                                Icon(
                                    imageVector = Icons.Outlined.GetApp,
                                    contentDescription = stringResource(Res.string.stub_text),
                                )
                            }
                        } else if (extension.loadIssue == ExtensionLoadIssue.QUARANTINED) {
                            TextButton(onClick = { onClickItemAction(extension) }) {
                                Text(text = stringResource(Res.string.retry))
                            }
                        }
                    }
                    is Extension.Available -> {
                        if (extension.sources.isNotEmpty()) {
                            IconButton(
                                onClick = { onClickItemSecondaryAction(extension) },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = stringResource(Res.string.stub_text),
                                )
                            }
                        }

                        IconButton(onClick = { onClickItemAction(extension) }) {
                            Icon(
                                imageVector = Icons.Outlined.GetApp,
                                contentDescription = stringResource(Res.string.ext_install),
                            )
                        }
                    }
                }
            }
        }
    }
}