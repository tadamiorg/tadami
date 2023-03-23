package com.sf.tadami.navigation.graphs

import androidx.navigation.*
import androidx.navigation.compose.composable
import com.sf.tadami.ui.discover.recent.RecentScreen
import com.sf.tadami.ui.discover.search.SearchScreen

fun NavGraphBuilder.discoverNavGraph(navController: NavHostController) {
    navigation(
        route = "DiscoverRoutes",
        startDestination = "${DiscoverRoutes.RECENT}/{sourceId}"
    ){
        composable(route = "${DiscoverRoutes.RECENT}/{sourceId}", arguments = listOf(
            navArgument("sourceId") { type = NavType.StringType }
        )) {
            RecentScreen(navController = navController)
        }
        composable(route = "${DiscoverRoutes.SEARCH}/{sourceId}",arguments = listOf(
            navArgument("sourceId") { type = NavType.StringType }
        )) {
            SearchScreen(navController = navController)
        }
    }
}
object DiscoverRoutes {
    const val RECENT = "recent"
    const val SEARCH = "search"
}