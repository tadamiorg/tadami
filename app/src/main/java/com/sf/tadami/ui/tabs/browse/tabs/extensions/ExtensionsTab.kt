package com.sf.tadami.ui.tabs.browse.tabs.extensions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.domain.extensions.Extension
import com.sf.tadami.navigation.graphs.discover.DiscoverRoutes
import com.sf.tadami.navigation.graphs.sources.SourcesRoutes
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.screens.ScreenTabContent
import com.sf.tadami.ui.main.MainActivity
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.ExtensionComponent
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.RemoveLegacyPackagesDialog
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.TrustExtensionDialog
import com.sf.tadami.ui.tabs.browse.tabs.extensions.components.UninstallExtensionDialog
import com.sf.tadami.ui.themes.colorschemes.active
import com.sf.tadami.utils.Lang
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun extensionsTab(
    navController: NavHostController,
    extensionsViewModel: ExtensionsViewModel = viewModel(LocalContext.current as MainActivity)
): ScreenTabContent {
    val uiState by extensionsViewModel.uiState.collectAsState()
    val sPrefs by rememberDataStoreState(customPrefs = SourcesPreferences).value.collectAsState()
    val filterTint =  if (sPrefs.enabledLanguages.size != Lang.getAllLangs().size) MaterialTheme.colorScheme.active else LocalContentColor.current

    return ScreenTabContent(
        titleRes = R.string.label_extensions,
        badgeNumber = uiState.updates.takeIf { it > 0 },
        searchEnabled = true,
        actions = listOf(
            Action.Vector(
                title = R.string.action_filter,
                icon = Icons.Outlined.FilterList,
                tint = filterTint,
                onClick = {
                    navController.navigate(DiscoverRoutes.EXTENSIONS_FILTER)
                },
            ),
            Action.CastButton()
        )
    ) { contentPadding, _ ->
        var uninstallDialog by remember { mutableStateOf<Extension.Installed?>(null) }
        var trustDialog by remember { mutableStateOf<Extension.Installed?>(null) }
        var showLegacyPackagesDialog by remember { mutableStateOf(false) }

        // Back from the uninstall dialog of the system for instance
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner.lifecycle) {
            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    extensionsViewModel.onResume()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        ExtensionComponent(
            state = uiState,
            contentPadding = contentPadding,
            searchQuery = uiState.searchQuery,
            onLongClickItem = { extension ->
                when (extension) {
                    is Extension.Available -> extensionsViewModel.installExtension(extension)
                    is Extension.Installed -> uninstallDialog = extension
                }
            },
            onClickItemCancel = extensionsViewModel::cancelInstallUpdateExtension,
            onClickUpdateAll = extensionsViewModel::updateAllExtensions,
            onOpenWebView = { extension ->
                extension.sources.getOrNull(0)?.let { source ->
                    val encodedUrl = URLEncoder.encode(
                        source.baseUrl,
                        StandardCharsets.UTF_8.toString()
                    )
                    navController.navigate("${SourcesRoutes.EXTENSIONS_WEBVIEW}/${source.id}/${source.name}/${encodedUrl}")
                }
            },
            onInstallExtension = extensionsViewModel::installExtension,
            onOpenExtension = {
                navController.navigate("${DiscoverRoutes.EXTENSION_DETAILS}/${it.pkgName}")
            },
            onUninstallExtension = { uninstallDialog = it },
            onTrustExtension = { trustDialog = it },
            onRetryExtension = extensionsViewModel::retryExtension,
            onUpdateExtension = extensionsViewModel::updateExtension,
            onRemoveLegacyPackages = { showLegacyPackagesDialog = true },
            onLoadExtensionsAgain = extensionsViewModel::loadExtensionsAgain,
            onRefresh = extensionsViewModel::findAvailableExtensions,
        )

        uninstallDialog?.let { extension ->
            UninstallExtensionDialog(
                extensionName = extension.name,
                onDismissRequest = { uninstallDialog = null },
                onConfirm = { extensionsViewModel.uninstallExtension(extension) },
            )
        }

        trustDialog?.let { extension ->
            TrustExtensionDialog(
                extension = extension,
                onDismissRequest = { trustDialog = null },
                onConfirm = { extensionsViewModel.trustExtension(extension) },
            )
        }

        if (showLegacyPackagesDialog) {
            RemoveLegacyPackagesDialog(
                onDismissRequest = { showLegacyPackagesDialog = false },
                onConfirm = extensionsViewModel::removeLegacyPackages,
            )
        }
    }
}