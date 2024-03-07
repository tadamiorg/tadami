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
import com.sf.tadami.navigation.graphs.app.animeInfos.AnimeInfosRoutes
import com.sf.tadami.navigation.graphs.app.discover.DiscoverRoutes

@Composable
fun GlobalSearchScreen(
    navController: NavHostController,
    globalSearchViewModel: GlobalSearchViewModel = viewModel()
) {
    var searchValue by rememberSaveable { mutableStateOf("") }
    val animesBySource by globalSearchViewModel.animesBySource.collectAsState()

    Scaffold(
        topBar = {
            GlobalSearchToolbar(
                onSearch = {
                    searchValue = it
                    globalSearchViewModel.search(it)
                },
                onSearchChange = {
                    searchValue = it
                    globalSearchViewModel.updateQuery(it)
                },
                onSearchCancel = {
                    navController.navigateUp()
                },
                searchValue = searchValue,
                progress = animesBySource.progress,
                total = animesBySource.total
            )
        },
    ) { innerPadding ->
        GlobalSearchComponent(
            modifier = Modifier.padding(innerPadding),
            animesBySource = animesBySource.items,
            onAnimeClicked = {
                navController.navigate("${AnimeInfosRoutes.DETAILS}/${it.source}/${it.id}")
            },
            onSourceClicked = {
                navController.navigate("${DiscoverRoutes.SEARCH}/${it.id}?basequery=$searchValue")
            }
        )
    }
}