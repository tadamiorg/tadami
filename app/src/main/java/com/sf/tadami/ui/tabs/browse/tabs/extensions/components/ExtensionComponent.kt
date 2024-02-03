package com.sf.tadami.ui.tabs.browse.tabs.extensions.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.ui.components.grid.EmptyScreen
import com.sf.tadami.ui.components.widgets.ContentLoader
import com.sf.tadami.ui.components.widgets.PullRefresh
import com.sf.tadami.ui.tabs.browse.tabs.extensions.ExtensionsUiState

@Composable
fun ExtensionComponent(
    state: ExtensionsUiState,
    contentPadding: PaddingValues,
    searchQuery: String?,
    onLongClickItem: (Extension) -> Unit,
    onClickItemCancel: (Extension) -> Unit,
    onOpenWebView: (Extension.Available) -> Unit,
    onInstallExtension: (Extension.Available) -> Unit,
    onUninstallExtension: (Extension) -> Unit,
    onUpdateExtension: (Extension.Installed) -> Unit,
    onOpenExtension: (Extension.Installed) -> Unit,
    onClickUpdateAll: () -> Unit,
    onRefresh: () -> Unit,
) {

    PullRefresh(
        refreshing = state.isRefreshing,
        onRefresh = onRefresh,
        enabled = !state.isLoading,
    ) {
        ContentLoader(isLoading = state.isLoading) {
            if (state.isEmpty) {
                val msg = if (!searchQuery.isNullOrEmpty()) {
                    R.string.pager_no_results
                } else {
                    R.string.ext_empty
                }
                EmptyScreen(
                    message = stringResource(id = msg),
                    modifier = Modifier.padding(contentPadding)
                )
            } else {
                ExtensionContent(
                    state = state,
                    contentPadding = contentPadding,
                    onLongClickItem = onLongClickItem,
                    onClickItemCancel = onClickItemCancel,
                    onOpenWebView = onOpenWebView,
                    onInstallExtension = onInstallExtension,
                    onUninstallExtension = onUninstallExtension,
                    onUpdateExtension = onUpdateExtension,
                    onOpenExtension = onOpenExtension,
                    onClickUpdateAll = onClickUpdateAll,
                )
            }
        }
    }
}