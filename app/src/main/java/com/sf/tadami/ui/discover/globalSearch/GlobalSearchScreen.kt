package com.sf.tadami.ui.discover.globalSearch

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.navigation.graphs.AnimeInfosRoutes
import com.sf.tadami.navigation.graphs.DiscoverRoutes
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.topappbar.search.SearchTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    navController : NavHostController,
    globalSearchViewModel: GlobalSearchViewModel = viewModel()
) {
    var searchValue by rememberSaveable { mutableStateOf("") }
    val animesBySource by globalSearchViewModel.animesBySource.collectAsState()

    Scaffold(
        topBar = {
            SearchTopAppBar(
                backHandlerEnabled = false,
                searchOpened = true,
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
                actions = listOf(
                    Action.CastButton()
                ),
                searchValue = searchValue
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