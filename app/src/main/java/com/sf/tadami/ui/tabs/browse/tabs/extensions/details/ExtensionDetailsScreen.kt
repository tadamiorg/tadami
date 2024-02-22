package com.sf.tadami.ui.tabs.browse.tabs.extensions.details

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.data.DropDownAction
import com.sf.tadami.ui.components.grid.EmptyScreen
import com.sf.tadami.ui.components.topappbar.TadaTopAppBar
import com.sf.tadami.ui.components.widgets.ContentLoader


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionDetailsScreen(
    navController: NavHostController,
    extensionDetailsViewModel: ExtensionDetailsViewModel = viewModel()
) {
    val uiState by extensionDetailsViewModel.uiState.collectAsState()

    ContentLoader(isLoading = uiState.isLoading) {
        val uriHandler = LocalUriHandler.current
        val url = remember(uiState.extension) {
            val regex = """https://raw.githubusercontent.com/(.+?)/(.+?)/.+""".toRegex()
            regex.find(uiState.extension?.repoUrl.orEmpty())
                ?.let {
                    val (user, repo) = it.destructured
                    "https://github.com/$user/$repo"
                }
                ?: uiState.extension?.repoUrl
        }
        Scaffold(
            topBar = {
                TadaTopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.label_extension_info),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    actions = listOf(

                        Action.Vector(
                            title = R.string.stub_text,
                            icon = Icons.AutoMirrored.Outlined.Launch,
                            onClick = {
                                if (url != null) {
                                    uriHandler.openUri(url)
                                }
                            }
                        ),
                        Action.DropDownDrawable(
                            title = R.string.stub_text,
                            icon = R.drawable.ic_vertical_settings,
                            items = listOf(
                                DropDownAction(
                                    title = stringResource(id = R.string.pref_clear_cookies),
                                    onClick = extensionDetailsViewModel::clearCookies,
                                )
                            )
                        )
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
                )
            }
        ) {
            if (uiState.extension == null) {
                EmptyScreen(
                    message = stringResource(id = R.string.empty_screen),
                    modifier = Modifier.padding(it),
                )
                return@Scaffold
            }
            ExtensionDetailsComponent(
                contentPadding = it,
                onClickUninstall = extensionDetailsViewModel::uninstallExtension,
                extension = uiState.extension!!
            )
        }
    }
}