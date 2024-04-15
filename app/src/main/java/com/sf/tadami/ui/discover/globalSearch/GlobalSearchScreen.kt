package com.sf.tadami.ui.discover.globalSearch

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.navigation.graphs.animeInfos.AnimeInfosRoutes
import com.sf.tadami.navigation.graphs.discover.DiscoverRoutes
import com.sf.tadami.ui.components.data.Action

@Composable
fun GlobalSearchScreen(
    navController: NavHostController,
    globalSearchViewModel: GlobalSearchViewModel = viewModel()
) {
    val uiState by globalSearchViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            GlobalSearchToolbar(
                onSearch = {
                    globalSearchViewModel.search()
                },
                onSearchChange = {
                    globalSearchViewModel.updateSearchQuery(it)
                },
                onSearchCancel = {
                    navController.navigateUp()
                },
                actions = listOf(
                    Action.CastButton()
                ),
                searchValue = uiState.searchQuery,
                progress = uiState.progress,
                total = uiState.total
            )
        },
    ) { innerPadding ->
        GlobalSearchComponent(
            modifier = Modifier.padding(innerPadding),
            animesBySource = uiState.items,
            onAnimeClicked = {
                navController.navigate("${AnimeInfosRoutes.DETAILS}/${it.source}/${it.id}")
            },
            onSourceClicked = {
                navController.navigate("${DiscoverRoutes.SEARCH}/${it.id}?initialQuery=${uiState.searchQuery}")
            }
        )
    }
}