package com.sf.tadami.ui.tabs.favorites

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.AnimeInfosRoutes
import com.sf.tadami.ui.components.bottombar.ContextualBottomBar
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.data.FavoriteItem
import com.sf.tadami.ui.components.topappbar.ContextualSearchTopAppBar
import com.sf.tadami.ui.tabs.favorites.bottomsheet.sortComparator
import com.sf.tadami.ui.tabs.favorites.bottomsheet.favoriteFilters
import com.sf.tadami.ui.tabs.settings.model.rememberDataStoreState
import com.sf.tadami.ui.tabs.settings.screens.library.LibraryPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavHostController,
    setNavDisplay: (display: Boolean) -> Unit,
    bottomNavDisplay: Boolean,
    showLibrarySheet: () -> Unit,
    favoritesViewModel: FavoritesViewModel = viewModel()
) {
    val context = LocalContext.current

    val favoriteList by favoritesViewModel.favoriteList.collectAsState()
    val searchFilter by favoritesViewModel.searchFilter.collectAsState()
    val libraryPreferences by rememberDataStoreState(customPrefs = LibraryPreferences).value.collectAsState()

    val actions = remember(libraryPreferences.filterFlags.isFiltered) {
        listOf(
            Action.Drawable(
                title = R.string.stub_text,
                icon = R.drawable.ic_filter,
                tint = if (libraryPreferences.filterFlags.isFiltered) Color.Yellow else null,
                onClick = {
                    showLibrarySheet()
                }
            )
        )
    }


    val isActionMode by remember(favoriteList) {
        derivedStateOf {
            val count = favoriteList.count { it.selected }
            if (count == 0) {
                setNavDisplay(true)
            }
            count
        }
    }

    var isSearchMode by remember {
        mutableStateOf(false)
    }

    val isRefreshing by favoritesViewModel.isRefreshing.collectAsState()


    Scaffold(
        topBar = {
            ContextualSearchTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.favorites_tab_title)
                    )
                },
                actions = actions,
                actionModeCounter = isActionMode,
                onCloseActionModeClicked = {
                    favoritesViewModel.toggleAllSelectedFavorites(false)
                },
                onToggleAll = {
                    favoritesViewModel.toggleAllSelectedFavorites(true)
                },
                onInverseAll = {
                    favoritesViewModel.inverseSelectedFavorites()
                },
                onSearchChange = {
                    favoritesViewModel.updateSearchFilter(it)
                },
                isSearchMode = isSearchMode,
                onSearchCancel = {
                    isSearchMode = false
                },
                onSearchClicked = {
                    isSearchMode = true
                }
            )
        },
        bottomBar = {
            ContextualBottomBar(
                visible = favoriteList.fastAny { it.selected } && !bottomNavDisplay,
                actions = listOf(
                    Action.Vector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.DoneAll,
                        onClick = {
                            favoritesViewModel.setSeenStatus(true)
                        },
                    ),
                    Action.Vector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.RemoveDone,
                        onClick = {
                            favoritesViewModel.setSeenStatus(false)
                        },
                    ),
                    Action.Vector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.DeleteOutline,
                        onClick = {
                            favoritesViewModel.unFavorite()
                        }
                    )
                )
            )
        },
    ) { innerPadding ->
        FavoritesComponent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            favoriteList = favoriteList.addFilters(libraryPreferences,searchFilter),
            onAnimeCLicked = { favorite ->
                when {
                    favorite.selected -> {
                        favoritesViewModel.toggleSelectedFavorite(favorite, false)
                    }
                    favoriteList.fastAny { it.selected } -> {
                        favoritesViewModel.toggleSelectedFavorite(favorite, true)
                    }
                    else -> {
                        navController.navigate("${AnimeInfosRoutes.DETAILS}/${favorite.anime.source}/${favorite.anime.id}")
                    }
                }
            },
            onAnimeLongCLicked = { favorite ->
                setNavDisplay(false)
                favoritesViewModel.toggleSelectedFavorite(favorite, true)
            },
            isRefreshing = isRefreshing,
            indicatorPadding = innerPadding,
            onRefresh = {
                favoritesViewModel.refreshAllFavorites(context)
            }
        )
    }
}

private fun List<FavoriteItem>.addFilters(prefs: LibraryPreferences,searchFilter : String): List<FavoriteItem> {
    return this
        .filter {
            it.anime.title.contains(searchFilter, true)
        }
        .favoriteFilters(prefs.filterFlags)
        .sortedWith { a1, a2 -> sortComparator(prefs.sortFlags).invoke(a1,a2) }
}
