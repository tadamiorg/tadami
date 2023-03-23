package com.sf.tadami.ui.discover.recent

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.AnimeInfosRoutes
import com.sf.tadami.ui.base.widgets.topbar.ScreenTopBar


@Composable
fun RecentScreen(
    navController: NavHostController,
    recentViewModel: RecentViewModel = viewModel()
) {

    val animeList = recentViewModel.animeList.collectAsLazyPagingItems()

    ScreenTopBar(
        title = "${stringResource(id = R.string.discover_recents_screen_title)} - ${recentViewModel.source.name}",
        backArrow = true,
        backArrowAction = { navController.navigateUp() }
    )
    {
        RecentComponent(
            animeList = animeList,
            onAnimeClicked = {
                navController.navigate("${AnimeInfosRoutes.DETAILS}/${it.source}/${it.id}")
            }
        )
    }
}