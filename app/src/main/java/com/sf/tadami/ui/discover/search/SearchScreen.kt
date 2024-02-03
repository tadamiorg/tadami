package com.sf.tadami.ui.discover.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.animeInfos.AnimeInfosRoutes
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.filters.TadaBottomSheetLayout
import com.sf.tadami.ui.components.topappbar.search.SearchTopAppBar
import com.sf.tadami.ui.utils.padding
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    baseQuery : String?,
    searchViewModel: SearchViewModel = viewModel()
) {
    val animeListState by searchViewModel.animeList.collectAsState()
    val animeList = animeListState.collectAsLazyPagingItems()
    val sourceFilters = searchViewModel.sourceFilters.collectAsState()
    val query by searchViewModel.query.collectAsState()

    var searchEnabled by rememberSaveable { mutableStateOf(false) }
    var hasBaseQuery by rememberSaveable { mutableStateOf(baseQuery!=null) }
    var isGlobalSearched by rememberSaveable { mutableStateOf(false) }
    if(hasBaseQuery){
        LaunchedEffect(Unit){
            if(baseQuery!=null){
                hasBaseQuery = false
                isGlobalSearched = true
                searchEnabled = true
                searchViewModel.resetData()
            }
        }
    }


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
                            Text(text = "${stringResource(id = R.string.discover_search_screen_title)} - ${searchViewModel.source.name}", overflow = TextOverflow.Ellipsis, maxLines = 1)
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.navigateUp() }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                    onSearchCancel = {
                        searchViewModel.updateQuery("")
                        searchViewModel.resetData()
                        searchEnabled = false
                    },
                    onSearchOpen = {
                        searchEnabled = true
                    },
                    searchOpened = searchEnabled,
                    onSearch = {
                        searchViewModel.resetData()
                    },
                    onSearchChange = {
                        searchViewModel.updateQuery(it)
                    },
                    actions = listOf(
                        Action.CastButton()
                    ),
                    searchValue = query,
                    backHandlerEnabled = !isGlobalSearched
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
                fabPadding = PaddingValues(bottom = heightInDp + MaterialTheme.padding.medium),
                animeList = animeList,
                snackbarHostState = snackbarHostState,
                onAnimeClicked = {
                    navController.navigate("${AnimeInfosRoutes.DETAILS}/${it.source}/${it.id}")
                }
            )
        }
    }
}
