package com.sf.tadami.ui.tabs.browse.tabs.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.ui.components.screens.ScreenTabContent
import com.sf.tadami.ui.main.MainActivity
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.ExtensionComponent

@Composable
fun extensionsTab(
    extensionsViewModel: ExtensionsViewModel = viewModel(LocalContext.current as MainActivity)
): ScreenTabContent {
    val uiState by extensionsViewModel.uiState.collectAsState()

    return ScreenTabContent(
        titleRes = R.string.label_extensions,
        badgeNumber = uiState.updates.takeIf { it > 0 },
        searchEnabled = true,
        actions = listOf()
    ) { contentPadding, _ ->
        ExtensionComponent(
            state = uiState,
            contentPadding = contentPadding,
            searchQuery = uiState.searchQuery,
            onLongClickItem = { extension ->
                when (extension) {
                    is Extension.Available -> extensionsViewModel.installExtension(extension)
                    else -> extensionsViewModel.uninstallExtension(extension)
                }
            },
            onClickItemCancel = extensionsViewModel::cancelInstallUpdateExtension,
            onClickUpdateAll = extensionsViewModel::updateAllExtensions,
            onOpenWebView = { extension ->
                extension.sources.getOrNull(0)?.let {

                }
            },
            onInstallExtension = extensionsViewModel::installExtension,
            onOpenExtension = {  },
            onUninstallExtension = { extensionsViewModel.uninstallExtension(it) },
            onUpdateExtension = extensionsViewModel::updateExtension,
            onRefresh = extensionsViewModel::findAvailableExtensions,
        )
    }
}