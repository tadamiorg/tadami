package com.sf.tadami.navigation.graphs.discover

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sf.tadami.ui.discover.globalSearch.GlobalSearchScreen
import com.sf.tadami.ui.discover.recent.RecentScreen
import com.sf.tadami.ui.discover.search.SearchScreen
import com.sf.tadami.ui.tabs.browse.filters.AnimeSourcesFilerScreen

fun NavGraphBuilder.discoverNavGraph(navController: NavHostController) {

    composable(
        route = "${DiscoverRoutes.RECENT}/{sourceId}",
        arguments = listOf(
            navArgument("sourceId") { type = NavType.LongType }
        )
    ) {
        RecentScreen(navController = navController)
    }
    composable(
        route = "${DiscoverRoutes.SEARCH}/{sourceId}?basequery={baseQuery}",
        arguments = listOf(
            navArgument("sourceId") { type = NavType.LongType },
            navArgument("baseQuery") {
                nullable = true
                defaultValue = null
                type = NavType.StringType
            }
        )
    ) {
        SearchScreen(
            navController = navController,
            baseQuery = it.arguments?.getString("baseQuery")
        )
    }
    composable(route = DiscoverRoutes.GLOBAL_SEARCH)
    {
        GlobalSearchScreen(navController = navController)
    }
    composable(route = DiscoverRoutes.SOURCES_FILTER)
    {
        AnimeSourcesFilerScreen(navController = navController)
    }
}

object DiscoverRoutes {
    const val RECENT = "recent"
    const val SEARCH = "search"
    const val GLOBAL_SEARCH = "global_search"
    const val SOURCES_FILTER = "sources_filter"
}