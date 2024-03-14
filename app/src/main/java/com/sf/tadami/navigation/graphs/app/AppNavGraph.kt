package com.sf.tadami.navigation.graphs.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.app.animeInfos.AnimeInfosRoutes
import com.sf.tadami.navigation.graphs.app.animeInfos.animeInfosNavGraph
import com.sf.tadami.navigation.graphs.app.discover.DiscoverRoutes
import com.sf.tadami.navigation.graphs.app.discover.discoverNavGraph
import com.sf.tadami.navigation.graphs.app.settings.nestedSettingsNavGraph
import com.sf.tadami.navigation.graphs.app.sources.nestedSourcesNavGraph
import com.sf.tadami.navigation.graphs.tabs.GRAPH
import com.sf.tadami.navigation.graphs.tabs.TabsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    var libraryFocusedAnime by remember { mutableLongStateOf(-1L) }
    NavHost(
        navController = navController,
        startDestination = GRAPH.TABS
    ) {
        composable(route = GRAPH.TABS){
            TabsScreen(
                openAnimeDetails = { source,anime ->
                    navController.navigate("${AnimeInfosRoutes.DETAILS}/${source}/${anime}")
                },
                openSourceSearch = {
                    navController.navigate("${DiscoverRoutes.SEARCH}/${it}")
                },
                libraryFocusedAnime = libraryFocusedAnime,
                setLibraryFocusedAnime = {
                    libraryFocusedAnime = it
                }
            )
        }
        animeInfosNavGraph(navController)
        discoverNavGraph(navController)
        nestedSettingsNavGraph(navController)
        nestedSourcesNavGraph(navController)
    }
}