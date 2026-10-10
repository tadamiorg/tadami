package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.ui.components.banners.WarningBanner
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn
import com.sf.tadami.ui.tabs.browse.tabs.extensions.ExtensionUiModel
import com.sf.tadami.ui.tabs.browse.tabs.extensions.ExtensionsUiState
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.item.ExtensionItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExtensionContent(
    state: ExtensionsUiState,
    contentPadding: PaddingValues,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUninstallExtension: (Extension) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onOpenExtension: (Extension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
    onRetryFailedExtension: (String) -> Unit,
    onUninstallFailedExtension: (String) -> Unit,
    onTrustFailedExtension: (String, String) -> Unit,
    onCleanupLegacyPackages: () -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        // Only shown to users upgrading from a build that installed extensions as apps.
        if (state.redundantPackages.isNotEmpty()) {
            item(key = "extension-legacy-cleanup") {
                WarningBanner(
                    textRes = R.string.ext_legacy_cleanup_banner,
                    modifier = Modifier.clickable { onCleanupLegacyPackages() },
                )
            }
        }

        if (state.failedExtensions.isNotEmpty()) {
            item(contentType = "header", key = "extensionHeader-failed") {
                ExtensionHeader(
                    textRes = R.string.ext_failed_header,
                    modifier = Modifier.animateItem(),
                )
            }
            items(
                items = state.failedExtensions,
                contentType = { "failed-item" },
                key = { "failed-extension-${it.pkgName}" },
            ) { error ->
                FailedExtensionItem(
                    error = error,
                    onRetry = onRetryFailedExtension,
                    onUninstall = onUninstallFailedExtension,
                    onTrust = onTrustFailedExtension,
                    modifier = Modifier.animateItem(),
                )
            }
        }

        state.items.forEach { (header, items) ->
            item(
                contentType = "header",
                key = "extensionHeader-${header.hashCode()}",
            ) {
                when (header) {
                    is ExtensionUiModel.Header.Resource -> {
                        val action: @Composable RowScope.() -> Unit =
                            if (header.textRes == R.string.ext_updates_pending) {
                                {
                                    Button(onClick = { onClickUpdateAll() }) {
                                        Text(
                                            text = stringResource(R.string.ext_update_all),
                                            style = LocalTextStyle.current.copy(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                        )
                                    }
                                }
                            } else {
                                {}
                            }
                        ExtensionHeader(
                            textRes = header.textRes,
                            modifier = Modifier.animateItem(),
                            action = action,
                        )
                    }
                    is ExtensionUiModel.Header.Text -> {
                        ExtensionHeader(
                            textRes = header.textRes,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            items(
                items = items,
                contentType = { "item" },
                key = { "extension-${it.hashCode()}" },
            ) { item ->
                ExtensionItem(
                    modifier = Modifier.animateItem(),
                    item = item,
                    onClickItem = {
                        when (it) {
                            is Extension.Available -> onInstallExtension(it)
                            is Extension.Installed -> onOpenExtension(it)
                        }
                    },
                    onLongClickItem = onLongClickItem,
                    onClickItemSecondaryAction = {
                        when (it) {
                            is Extension.Available -> onOpenWebView(it)
                            is Extension.Installed -> onUninstallExtension(it)
                        }
                    },
                    onClickItemCancel = onClickItemCancel,
                    onClickItemAction = {
                        when (it) {
                            is Extension.Available -> onInstallExtension(it)
                            is Extension.Installed -> {
                                if (it.hasUpdate) {
                                    onUpdateExtension(it)
                                } else {
                                    onOpenExtension(it)
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}