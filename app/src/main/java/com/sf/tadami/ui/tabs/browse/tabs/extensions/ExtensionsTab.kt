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
import com.sf.tadami.ui.tabs.browse.tabs.extensions.dialogs.ExtensionDialog
import com.sf.tadami.ui.tabs.browse.tabs.extensions.dialogs.InstallExtensionDialog
import com.sf.tadami.ui.tabs.browse.tabs.extensions.dialogs.RemoveLegacyPackagesDialog
import com.sf.tadami.ui.tabs.browse.tabs.extensions.dialogs.UninstallExtensionDialog
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
        var dialog by remember { mutableStateOf<ExtensionDialog?>(null) }

        // The system uninstall dialog runs outside this app, so the only reliable moment to find
        // out whether the user went through with it is when we come back to the foreground.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner.lifecycle) {
            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    extensionsViewModel.refreshRedundantPackages()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        ExtensionComponent(
            state = uiState,
            contentPadding = contentPadding,
            searchQuery = uiState.searchQuery,
            onLongClickItem = { extension ->
                dialog = when (extension) {
                    is Extension.Available -> ExtensionDialog.Install(extension)
                    else -> ExtensionDialog.Uninstall(extension.name, extension.pkgName)
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
            onInstallExtension = { dialog = ExtensionDialog.Install(it) },
            onOpenExtension = {
                navController.navigate("${DiscoverRoutes.EXTENSION_DETAILS}/${it.pkgName}")
            },
            onUninstallExtension = { dialog = ExtensionDialog.Uninstall(it.name, it.pkgName) },
            onUpdateExtension = extensionsViewModel::updateExtension,
            onRefresh = extensionsViewModel::findAvailableExtensions,
            onRetryFailedExtension = extensionsViewModel::retryFailedExtension,
            onUninstallFailedExtension = { dialog = ExtensionDialog.Uninstall(it, it) },
            onTrustFailedExtension = extensionsViewModel::trustExtension,
            onCleanupLegacyPackages = {
                dialog = ExtensionDialog.RemoveLegacyPackages(uiState.redundantPackages.size)
            },
        )

        when (val current = dialog) {
            is ExtensionDialog.Install -> InstallExtensionDialog(
                extensionName = current.extension.name,
                onDismissRequest = { dialog = null },
                onConfirm = { extensionsViewModel.installExtension(current.extension) },
            )

            is ExtensionDialog.Uninstall -> UninstallExtensionDialog(
                extensionName = current.name,
                onDismissRequest = { dialog = null },
                onConfirm = { extensionsViewModel.uninstallExtension(current.pkgName) },
            )

            is ExtensionDialog.RemoveLegacyPackages -> RemoveLegacyPackagesDialog(
                count = current.count,
                onDismissRequest = { dialog = null },
                onConfirm = extensionsViewModel::removeAllLegacyPackages,
            )

            null -> Unit
        }
    }
}