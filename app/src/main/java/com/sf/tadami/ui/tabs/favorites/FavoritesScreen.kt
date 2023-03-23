package com.sf.tadami.ui.tabs.favorites

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.util.fastAny
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.AnimeInfosRoutes
import com.sf.tadami.ui.components.ContextualBottomBar
import com.sf.tadami.ui.components.toolbar.Action
import com.sf.tadami.ui.components.toolbar.ContextualTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavHostController,
    setNavDisplay : (display : Boolean) -> Unit,
    bottomNavDisplay : Boolean,
    favoritesViewModel: FavoritesViewModel = viewModel()
) {
    val context = LocalContext.current

    val favoriteList by favoritesViewModel.favoriteList.collectAsState()

    val actions = listOf(
        Action.Drawable(
            title = R.string.stub_text,
            icon = R.drawable.ic_search,
            onClick = {

            }
        ),
        Action.Drawable(
            title = R.string.stub_text,
            icon = R.drawable.ic_filter,
            onClick = {

            }
        )
    )

    val isActionMode by remember(favoriteList) {
        derivedStateOf {
            val count = favoriteList.count { it.selected }
            if(count == 0){
                setNavDisplay(true)
            }
            count
        }
    }

    val isRefreshing by favoritesViewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            ContextualTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.favorites_tab_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = actions,
                actionModeCounter = isActionMode,
                onCloseClicked = {
                    favoritesViewModel.toggleAllSelectedFavorites(false)
                },
                onToggleAll = {
                    favoritesViewModel.toggleAllSelectedFavorites(true)
                },
                onInverseAll = {
                    favoritesViewModel.inverseSelectedFavorites()
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
        }
    ) { innerPadding ->
        FavoritesComponent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            favoriteList = favoriteList,
            onAnimeCLicked = { favorite ->
                when{
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