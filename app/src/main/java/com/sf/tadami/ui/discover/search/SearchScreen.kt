package com.sf.tadami.ui.discover.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.*
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
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.AnimeInfosRoutes
import com.sf.tadami.ui.components.filters.TadaBottomSheetLayout
import com.sf.tadami.ui.components.topappbar.search.SearchTopAppBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    searchViewModel: SearchViewModel = viewModel()
) {
    val animeListState by searchViewModel.animeList.collectAsState()
    val animeList = animeListState.collectAsLazyPagingItems()
    val sourceFilters = searchViewModel.sourceFilters.collectAsState()

    var searchEnabled by rememberSaveable { mutableStateOf(false) }
    var searchValue by rememberSaveable { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    val filtersSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false
    )

    var fabHeight by remember {
        mutableStateOf(0)
    }

    val heightInDp = with(LocalDensity.current) { fabHeight.toDp() }

    val snackbarHostState = remember { SnackbarHostState() }

    TadaBottomSheetLayout(
        sheetContent = {
            FiltersSheet(
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
    ) {
        Scaffold(
            topBar = {
                SearchTopAppBar(
                    title = {
                            Text(text = "${stringResource(id = R.string.discover_search_screen_title)} - ${searchViewModel.source.name}")
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.navigateUp() }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                    onSearchCancel = {
                        searchViewModel.updateQuery("")
                        searchViewModel.resetData()
                        searchEnabled = false
                        searchValue = ""
                    },
                    onSearchOpen = {
                        searchEnabled = true
                    },
                    searchOpened = searchEnabled,
                    onSearch = {
                        searchViewModel.resetData()
                    },
                    onSearchChange = {
                        searchValue = it
                        searchViewModel.updateQuery(it)
                    },
                    searchValue = searchValue
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
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState)}
        ) { innerPadding ->
            SearchComponent(
                modifier = Modifier.padding(innerPadding),
                fabPadding = PaddingValues(bottom = heightInDp + 16.dp),
                animeList = animeList,
                snackbarHostState = snackbarHostState,
                onAnimeClicked = {
                    navController.navigate("${AnimeInfosRoutes.DETAILS}/${it.source}/${it.id}")
                }
            )
        }
    }
}
