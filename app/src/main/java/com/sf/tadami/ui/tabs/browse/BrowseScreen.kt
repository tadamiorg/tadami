package com.sf.tadami.ui.tabs.browse

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.ui.components.screens.TabbedScreen
import com.sf.tadami.ui.main.MainActivity
import com.sf.tadami.ui.tabs.browse.tabs.extensions.ExtensionsViewModel
import com.sf.tadami.ui.tabs.browse.tabs.extensions.extensionsTab
import com.sf.tadami.ui.tabs.browse.tabs.sources.sourcesTab

@Composable
fun BrowseScreen(
    modifier : Modifier = Modifier,
    navController : NavHostController,
    extensionsViewModel : ExtensionsViewModel = viewModel(LocalActivity.current as MainActivity)
) {
    // Hoisted for extensions tab's search bar
    val extensionsState by extensionsViewModel.uiState.collectAsState()

    TabbedScreen(
        modifier = modifier,
        titleRes = R.string.browse_tab_title,
        tabs = listOf(
            sourcesTab(navController),
            extensionsTab(navController)
        ),
        searchValue = extensionsState.searchQuery,
        onSearchChange = extensionsViewModel::search,
    )
}