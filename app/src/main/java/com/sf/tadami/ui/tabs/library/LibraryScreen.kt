package com.sf.tadami.ui.tabs.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.animeInfos.AnimeInfosRoutes
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.ui.components.bottombar.ContextualBottomBar
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.data.LibraryItem
import com.sf.tadami.ui.components.topappbar.ContextualSearchTopAppBar
import com.sf.tadami.ui.tabs.library.bottomsheet.libraryFilters
import com.sf.tadami.ui.tabs.library.bottomsheet.sortComparator
import com.sf.tadami.ui.themes.colorschemes.active
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    setNavDisplay: (display: Boolean) -> Unit,
    bottomNavDisplay: Boolean,
    showLibrarySheet: () -> Unit,
    librarySheetVisible: Boolean,
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val context = LocalContext.current

    val initLoaded by libraryViewModel.initLoaded.collectAsState()
    val libraryList by libraryViewModel.libraryList.collectAsState()
    val searchFilter by libraryViewModel.searchFilter.collectAsState()
    val libraryPreferences by rememberDataStoreState(customPrefs = LibraryPreferences).value.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val filterTint = if (libraryPreferences.filterFlags.isFiltered) MaterialTheme.colorScheme.active else LocalContentColor.current
    val actions = remember(libraryPreferences.filterFlags.isFiltered) {
        listOf(
            Action.Drawable(
                title = R.string.stub_text,
                icon = R.drawable.ic_filter,
                tint = filterTint,
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    showLibrarySheet()
                }
            ),
            Action.CastButton()
        )
    }


    val isActionMode by remember(libraryList) {
        derivedStateOf {
            val count = libraryList.count { it.selected }
            if (count == 0) {
                setNavDisplay(true)
            }
            count
        }
    }

    var isSearchMode by rememberSaveable {
        mutableStateOf(false)
    }

    val isRefreshing by libraryViewModel.isRefreshing.collectAsState()


    Scaffold(
        modifier = modifier,
        topBar = {
            ContextualSearchTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.library_tab_title)
                    )
                },
                actions = actions,
                actionModeCounter = isActionMode,
                onCloseActionModeClicked = {
                    libraryViewModel.toggleAllSelected(false)
                },
                onToggleAll = {
                    libraryViewModel.toggleAllSelected(true)
                },
                onInverseAll = {
                    libraryViewModel.inverseSelected()
                },
                onSearchChange = {
                    libraryViewModel.updateSearchFilter(it)
                },
                isSearchMode = isSearchMode,
                onSearchCancel = {
                    isSearchMode = false
                },
                backHandlerEnabled = !librarySheetVisible,
                onSearchClicked = {
                    isSearchMode = true
                }
            )
        },
        bottomBar = {
            ContextualBottomBar(
                visible = libraryList.fastAny { it.selected } && !bottomNavDisplay,
                actions = listOf(
                    Action.Vector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.DoneAll,
                        onClick = {
                            libraryViewModel.setSeenStatus(true)
                        },
                    ),
                    Action.Vector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.RemoveDone,
                        onClick = {
                            libraryViewModel.setSeenStatus(false)
                        },
                    ),
                    Action.Vector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.DeleteOutline,
                        onClick = {
                            libraryViewModel.unFavorite()
                        }
                    )
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { innerPadding ->
        LibraryComponent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            libraryList = libraryList.addFilters(libraryPreferences, searchFilter),
            librarySize = libraryList.size,
            initLoaded = initLoaded,
            onAnimeClicked = { libraryItem ->
                when {
                    libraryItem.selected -> {
                        libraryViewModel.toggleSelected(libraryItem, false)
                    }

                    libraryList.fastAny { it.selected } -> {
                        libraryViewModel.toggleSelected(libraryItem, true)
                    }

                    else -> {
                        navController.navigate("${AnimeInfosRoutes.DETAILS}/${libraryItem.anime.source}/${libraryItem.anime.id}")
                    }
                }
            },
            onAnimeLongCLicked = { libraryItem ->
                setNavDisplay(false)
                libraryViewModel.toggleSelected(libraryItem, true)
            },
            isRefreshing = isRefreshing,
            indicatorPadding = innerPadding,
            onRefresh = {
                val started = libraryViewModel.refreshLibrary(context)
                val msgRes = if (started) context.getString(R.string.update_starting) else context.getString(R.string.update_running)
                coroutineScope.launch {
                    snackBarHostState.currentSnackbarData?.dismiss()
                    snackBarHostState.showSnackbar(msgRes)
                }
            },
            onEmptyRefreshClicked = {
                navController.navigate(HomeNavItems.Sources.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }

            }
        )
    }
}

private fun List<LibraryItem>.addFilters(
    prefs: LibraryPreferences,
    searchFilter: String
): List<LibraryItem> {
    return this
        .filter {
            it.anime.title.contains(searchFilter, true)
        }
        .libraryFilters(prefs.filterFlags)
        .sortedWith { a1, a2 -> sortComparator(prefs.sortFlags).invoke(a1, a2) }
}
