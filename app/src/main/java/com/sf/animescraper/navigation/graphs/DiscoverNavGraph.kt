package com.sf.animescraper.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.sf.animescraper.ui.discover.recent.RecentScreen
import com.sf.animescraper.ui.discover.search.SearchScreen

fun NavGraphBuilder.discoverNavGraph(navController: NavHostController) {
    navigation(
        route = "DiscoverRoutes",
        startDestination = DiscoverRoutes.RECENT
    ){
        composable(route = DiscoverRoutes.RECENT) {
            RecentScreen(navController = navController)
        }
        composable(route = DiscoverRoutes.SEARCH) {
            SearchScreen(navController = navController)
        }
    }

}
object DiscoverRoutes {
    const val RECENT = "recent"
    const val SEARCH = "search"
}