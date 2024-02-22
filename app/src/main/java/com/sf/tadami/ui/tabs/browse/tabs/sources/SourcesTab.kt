package com.sf.tadami.ui.tabs.browse.tabs.sources

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.discover.DiscoverRoutes
import com.sf.tadami.navigation.graphs.sources.SourcesRoutes
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.screens.ScreenTabContent
import com.sf.tadami.ui.tabs.browse.tabs.sources.components.SourcesComponent
import com.sf.tadami.ui.themes.colorschemes.active
import com.sf.tadami.utils.Lang

@Composable
fun sourcesTab(
    navController: NavHostController,
    sourcesTabViewModel: SourcesTabViewModel = viewModel()
): ScreenTabContent {
    val uiState by sourcesTabViewModel.uiState.collectAsState()

    val sPrefs by rememberDataStoreState(customPrefs = SourcesPreferences).value.collectAsState()
    val filterTint =  if (sPrefs.hiddenSources.isNotEmpty() || sPrefs.enabledLanguages.size != Lang.getAllLangs().size) MaterialTheme.colorScheme.active else LocalContentColor.current

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
                tint = filterTint,
                onClick = {
                    navController.navigate(DiscoverRoutes.SOURCES_FILTER)
                },
            ),
            Action.CastButton()
        ),
        content = { contentPadding, snackbarHostState ->
            SourcesComponent(
                uiState = uiState,
                contentPadding = contentPadding,
                onClickItem = { source ->
                    navController.navigate("${DiscoverRoutes.SEARCH}/${source.id}")
                },
                onRecentClicked = { source ->
                    navController.navigate("${DiscoverRoutes.RECENT}/${source.id}")
                },
                onOptionsClicked = { source ->
                    navController.navigate("${SourcesRoutes.SETTINGS}/${source.id}")
                },
            )
        },
    )
}
