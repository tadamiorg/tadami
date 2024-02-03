package com.sf.tadami.ui.tabs.browse.tabs.sources

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.discover.DiscoverRoutes
import com.sf.tadami.navigation.graphs.sources.SourcesRoutes
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.screens.ScreenTabContent
import com.sf.tadami.ui.tabs.browse.tabs.sources.components.SourcesComponent

@Composable
fun sourcesTab(
    navController: NavHostController,
    sourcesTabViewModel: SourcesTabViewModel = viewModel()
): ScreenTabContent {
    val uiState by sourcesTabViewModel.uiState.collectAsState()

    return ScreenTabContent(
        titleRes = R.string.label_sources,
        actions = listOf(
            Action.Vector(
                title = R.string.stub_text,
                icon = Icons.Outlined.TravelExplore,
                onClick = {
                    navController.navigate(DiscoverRoutes.GLOBAL_SEARCH)
                },
            ),
            Action.Vector(
                title = R.string.action_filter,
                icon = Icons.Outlined.FilterList,
                onClick = {
                    navController.navigate(DiscoverRoutes.SOURCES_FILTER)
                },
            ),
        ),
        content = { contentPadding, snackbarHostState ->
            SourcesComponent(
                uiState = uiState,
                contentPadding = contentPadding,
                onClickItem = { source ->

                },
                onOptionsClicked = {source ->
                    navController.navigate("${SourcesRoutes.SETTINGS}/${source.id}")
                },
            )
        },
    )
}
