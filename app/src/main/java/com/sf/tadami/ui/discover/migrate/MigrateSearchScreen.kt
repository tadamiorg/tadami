package com.sf.tadami.ui.discover.migrate

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.sf.tadami.ui.discover.globalSearch.GlobalSearchComponent
import com.sf.tadami.ui.discover.globalSearch.GlobalSearchToolbar

@Composable
fun MigrateSearchScreen(
    navController: NavHostController,
    migrateViewModel: MigrateViewModel = viewModel()
) {

    val uiState by migrateViewModel.uiState.collectAsState()
    val searchValue by migrateViewModel.query.collectAsState()

    Scaffold(
        topBar = {
            GlobalSearchToolbar(
                searchValue = searchValue,
                progress = uiState.progress,
                total = uiState.total,
                navigateUp = navigateUp,
                onSearchChange = onChangeSearchQuery,
                onSearch = onSearch,
                onChangeSearchFilter = onChangeSearchFilter,
                onToggleResults = onToggleResults,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        GlobalSearchComponent(
            fromSourceId = fromSourceId,
            items = state.filteredItems,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}
