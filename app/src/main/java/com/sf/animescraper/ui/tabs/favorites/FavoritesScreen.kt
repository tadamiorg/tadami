package com.sf.animescraper.ui.tabs.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.animescraper.R
import com.sf.animescraper.navigation.graphs.AnimeInfosRoutes
import com.sf.animescraper.ui.base.widgets.topbar.ActionItem
import com.sf.animescraper.ui.components.toolbar.Action

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    navController: NavHostController,
    favoritesViewModel: FavoritesViewModel = viewModel()
) {

    val favoriteList by favoritesViewModel.favoriteList.collectAsState()

    val actions = listOf(
        Action.Drawable(
            title = R.string.stub_text,
            icon = R.drawable.ic_search,
            onClick = {}
        ),
        Action.Drawable(
            title = R.string.stub_text,
            icon = R.drawable.ic_filter,
            onClick = {}
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.favorites_tab_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    actions.forEach {
                        ActionItem(it)
                    }
                }
            )
        },
    ) {
        FavoritesComponent(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            favoriteList = favoriteList,
            onFavoriteClicked = { favorite ->
                navController.navigate("${AnimeInfosRoutes.DETAILS}/${favorite.source}/${favorite.id}")
            }
        )
    }
}