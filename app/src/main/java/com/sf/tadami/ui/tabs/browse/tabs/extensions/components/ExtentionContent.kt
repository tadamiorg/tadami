package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.sf.tadami.extension.model.ExtensionLoadIssue
import com.sf.tadami.ui.components.banners.WarningBanner
import com.sf.tadami.ui.components.widgets.FastScrollLazyColumn
import com.sf.tadami.ui.tabs.browse.tabs.extensions.ExtensionUiModel
import com.sf.tadami.ui.tabs.browse.tabs.extensions.ExtensionsUiState
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.item.ExtensionItem
import com.sf.tadami.ui.utils.padding

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExtensionContent(
    state: ExtensionsUiState,
    contentPadding: PaddingValues,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUninstallExtension: (Extension.Installed) -> Unit,
    onTrustExtension: (Extension.Installed) -> Unit,
    onRetryExtension: (Extension.Installed) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onOpenExtension: (Extension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
    onRemoveLegacyPackages: () -> Unit,
    onLoadExtensionsAgain: () -> Unit,
) {
    FastScrollLazyColumn(
        contentPadding = contentPadding,
    ) {
        if (state.safeMode) {
            item(key = "extension-safe-mode") {
                Column {
                    WarningBanner(textRes = R.string.ext_safe_mode_banner)
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MaterialTheme.padding.medium,
                                vertical = MaterialTheme.padding.small,
                            ),
                        onClick = onLoadExtensionsAgain,
                    ) {
                        Text(
                            text = stringResource(R.string.ext_safe_mode_load),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }

        if (state.legacyPackages.isNotEmpty()) {
            item(key = "extension-legacy-packages") {
                WarningBanner(
                    textRes = R.string.ext_legacy_packages_banner,
                    modifier = Modifier.clickable { onRemoveLegacyPackages() },
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
                                if (it.loadIssue == ExtensionLoadIssue.UNTRUSTED) {
                                    onTrustExtension(it)
                                } else if (it.hasUpdate) {
                                    onUpdateExtension(it)
                                } else if (it.loadIssue == ExtensionLoadIssue.QUARANTINED) {
                                    onRetryExtension(it)
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