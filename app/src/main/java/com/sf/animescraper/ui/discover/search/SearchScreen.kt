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
import com.sf.animescraper.R
import com.sf.animescraper.navigation.graphs.AnimeInfosRoutes
import com.sf.animescraper.network.requests.okhttp.Callback
import com.sf.animescraper.network.scraping.AnimesPage
import com.sf.animescraper.ui.components.filters.AsBottomSheetLayout
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    searchViewModel: SearchViewModel = viewModel()
) {

    val searchUiState by searchViewModel.uiState.collectAsState()
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var searchEnabled by rememberSaveable { mutableStateOf(false) }
    val sourceFilters = searchViewModel.sourceFilters.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val filtersSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden, skipHalfExpanded = false)

    var fabHeight by remember {
        mutableStateOf(0)
    }

    fun search(){
        isLoading = true
        searchViewModel.resetData()
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
                    search()
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
                        search()
                        searchEnabled = false
                    },
                    onSearchClicked = {
                        searchEnabled = true
                    },
                    searchEnabled = searchEnabled,
                    onSearch = {
                        search()
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
                animeList = searchUiState.animeList,
                onAnimeClicked = {
                    searchViewModel.onAnimeClicked(it)
                    navController.navigate(AnimeInfosRoutes.DETAILS)
                },
                onLoad = {
                    if (searchUiState.hasNextPage) {
                        searchViewModel.getSearch(object : Callback<AnimesPage> {
                            override fun onData(data: AnimesPage?) {
                                isLoading = false
                            }

                            override fun onError(message: String?, errorCode: Int?) {
                                isLoading = false
                            }
                        })
                    }

                },
                isLoading = isLoading,
            )
        }
    }
}
