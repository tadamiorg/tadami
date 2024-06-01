package com.sf.tadami.ui.discover.search

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Public
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.animeInfos.AnimeInfosRoutes
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.navigation.graphs.sources.SourcesRoutes
import com.sf.tadami.source.online.AnimeHttpSource
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.filters.TadaBottomSheetLayout
import com.sf.tadami.ui.components.topappbar.search.SearchTopAppBar
import com.sf.tadami.ui.discover.migrate.dialog.MigrateDialog
import com.sf.tadami.ui.discover.migrate.dialog.MigrationState
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.padding
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    searchViewModel: SearchViewModel = viewModel()
) {
    val animeListState by searchViewModel.animeList.collectAsState()
    val animeList = animeListState.collectAsLazyPagingItems()
    val sourceFilters = searchViewModel.sourceFilters.collectAsState()
    val queryState by searchViewModel.queryState.collectAsState()
    val migrateHelperState by searchViewModel.migrateHelperState.collectAsState()
    var searchEnabled by rememberSaveable { mutableStateOf(queryState.query.isNotEmpty()) }

    val coroutineScope = rememberCoroutineScope()

    val filtersSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false
    )

    var fabHeight by remember {
        mutableIntStateOf(0)
    }

    val heightInDp = with(LocalDensity.current) { fabHeight.toDp() }

    val snackbarHostState = remember { SnackbarHostState() }

    var isMigrationOpened by rememberSaveable {
        mutableStateOf(false)
    }



    Box {
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
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = stringResource(id = R.string.discover_search_screen_title),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start,

                                    )
                                Text(
                                    text = searchViewModel.source.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start,
                                )

                            }
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
                            Action.Vector(
                                title = R.string.stub_text,
                                icon = Icons.Outlined.Public,
                                onClick = {
                                    val httpSource = searchViewModel.source as? AnimeHttpSource
                                    httpSource?.let{
                                        val encodedUrl = URLEncoder.encode(
                                            it.baseUrl,
                                            StandardCharsets.UTF_8.toString()
                                        )
                                        navController.navigate("${SourcesRoutes.EXTENSIONS_WEBVIEW}/${it.id}/${it.name}/${encodedUrl}")
                                    }
                                }
                            ),
                            Action.CastButton()
                        ),
                        searchValue = queryState.query,
                        backHandlerEnabled = !queryState.fromGlobalSearch
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
                    onAnimeLongClicked = {
                        if(migrateHelperState.oldAnime != null){
                            navController.navigate("${AnimeInfosRoutes.DETAILS}/${it.source}/${it.id}?migrationId=${migrateHelperState.oldAnime!!.id}")
                        }else{
                            navController.navigate("${AnimeInfosRoutes.DETAILS}/${it.source}/${it.id}")
                        }

                    },
                    onAnimeClicked = {
                        if(migrateHelperState.oldAnime != null){
                            searchViewModel.setClickedAnime(it)
                            isMigrationOpened = true
                        }else{
                            navController.navigate("${AnimeInfosRoutes.DETAILS}/${it.source}/${it.id}")
                        }
                    }
                )
            }
        }
        MigrateDialog(
            opened = isMigrationOpened,
            oldAnime = migrateHelperState.oldAnime,
            newAnime = migrateHelperState.newAnime,
            onClickTitle = {
                if(migrateHelperState.newAnime != null){
                    navController.navigate("${AnimeInfosRoutes.DETAILS}/${migrateHelperState.newAnime!!.source}/${migrateHelperState.newAnime!!.id}?migrationId=${migrateHelperState.oldAnime!!.id}")
                }
            },
            onMigrate = {
                navController.popBackStack(
                    HomeNavItems.Library.route,
                    inclusive = false
                )
                if(it == MigrationState.ERRORED){
                    UiToasts.showToast(R.string.migration_error, Toast.LENGTH_LONG)
                    navController.navigate("${AnimeInfosRoutes.DETAILS}/${migrateHelperState.oldAnime!!.source}/${migrateHelperState.oldAnime!!.id}")
                }
                else{
                    navController.navigate("${AnimeInfosRoutes.DETAILS}/${migrateHelperState.newAnime!!.source}/${migrateHelperState.newAnime!!.id}")
                }
            },
            onDismissRequest = {
                isMigrationOpened = false
                searchViewModel.setClickedAnime(null)
            }
        )
    }
}
