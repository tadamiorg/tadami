package com.sf.animescraper.ui.discover.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import com.sf.animescraper.R
import com.sf.animescraper.navigation.graphs.AnimeInfosRoutes
import com.sf.animescraper.ui.components.filters.AsBottomSheetLayout
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    searchViewModel: SearchViewModel = viewModel()
) {
    val animeListState by searchViewModel.animeList.collectAsState()
    val animeList = animeListState.collectAsLazyPagingItems()
    var searchEnabled by rememberSaveable { mutableStateOf(false) }
    val sourceFilters = searchViewModel.sourceFilters.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val filtersSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = false)

    var fabHeight by remember {
        mutableStateOf(0)
    }

    val heightInDp = with(LocalDensity.current) { fabHeight.toDp() }

    AsBottomSheetLayout(
        sheetContent = {
            FiltersSheet(
                hideSheet = {
                    coroutineScope.launch {
                        filtersSheetState.hide()
                    }
                },
                isVisible = filtersSheetState.targetValue != ModalBottomSheetValue.Hidden || (filtersSheetState.progress.to != filtersSheetState.progress.from),
                filters = sourceFilters.value,
                onUpdateFilters = {
                    searchViewModel.updateFilters(it)
                },
                onResetClicked = {
                    searchViewModel.resetFilters()
                },
                search = {
                    searchViewModel.resetData()
                }
            )
        },
        sheetState = filtersSheetState,
        onScrimClicked = {
            coroutineScope.launch {
                filtersSheetState.hide()
            }
        }
    ) {
        Scaffold(
            topBar = {
                SearchToolBar(
                    title = "${stringResource(id = R.string.discover_search_screen_title)} - ${searchViewModel.source.name}",
                    onBackClicked = { navController.navigateUp() },
                    onCancelActionMode = {
                        searchViewModel.updateQuery("")
                        searchViewModel.resetData()
                        searchEnabled = false
                    },
                    onSearchClicked = {
                        searchEnabled = true
                    },
                    searchEnabled = searchEnabled,
                    onSearch = {
                        searchViewModel.resetData()
                    },
                    onUpdateSearch = {
                        searchViewModel.updateQuery(it)
                    }
                )
            },
            floatingActionButton = {
                if (sourceFilters.value.isNotEmpty()) {
                    FloatingActionButton(
                        modifier = Modifier.onGloballyPositioned {
                            fabHeight = it.size.height
                        },
                        onClick = {
                            coroutineScope.launch {
                                filtersSheetState.show()
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_filter),
                            contentDescription = null
                        )
                    }
                }
            }
        ) { innerPadding ->
            SearchComponent(
                modifier = Modifier.padding(innerPadding),
                fabPadding = PaddingValues(bottom = heightInDp + 16.dp),
                animeList = animeList,
                onAnimeClicked = {
                    navController.navigate("${AnimeInfosRoutes.DETAILS}/${it.source}/${it.id}")
                }
            )
        }
    }
}
