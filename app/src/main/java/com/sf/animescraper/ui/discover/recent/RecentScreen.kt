package com.sf.animescraper.ui.discover.recent

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.animescraper.R
import com.sf.animescraper.navigation.graphs.AnimeInfosRoutes
import com.sf.animescraper.network.requests.okhttp.Callback
import com.sf.animescraper.network.scraping.AnimesPage
import com.sf.animescraper.ui.base.widgets.topbar.ScreenTopBar
import com.sf.animescraper.ui.shared.SharedViewModel
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun RecentScreen(
    navController: NavHostController,
    recentViewModel: RecentViewModel = viewModel()
) {

    val recentUiState by recentViewModel.uiState.collectAsState()
    var isLoading by rememberSaveable { mutableStateOf(true) }

    ScreenTopBar(
        title = "${stringResource(id = R.string.discover_recents_screen_title)} - ${recentViewModel.source.name}",
        backArrow = true,
        backArrowAction = { navController.navigateUp() }
    )
    {
        RecentComponent(
            animeList = recentUiState.animeList,
            onAnimeClicked = {
                recentViewModel.onAnimeClicked(it)
                navController.navigate(AnimeInfosRoutes.DETAILS)
            },
            onLoad = {
                if(recentUiState.hasNextPage){
                    recentViewModel.getRecent(object : Callback<AnimesPage> {
                        override fun onData(data: AnimesPage?) {
                            isLoading = false
                        }
                        override fun onError(message: String?, errorCode: Int?) {
                            isLoading = false
                        }
                    })
                }
            },
            isLoading = isLoading
        )
    }
}