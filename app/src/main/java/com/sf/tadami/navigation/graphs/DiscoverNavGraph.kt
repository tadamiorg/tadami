package com.sf.tadami.navigation.graphs

import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.navigation.*
import androidx.navigation.compose.composable
import com.sf.tadami.ui.discover.globalSearch.GlobalSearchScreen
import com.sf.tadami.ui.discover.recent.RecentScreen
import com.sf.tadami.ui.discover.search.SearchScreen

fun NavGraphBuilder.discoverNavGraph(navController: NavHostController) {
    navigation(
        route = "DiscoverRoutes",
        startDestination = "${DiscoverRoutes.RECENT}/{sourceId}"
    ) {
        composable(
            route = "${DiscoverRoutes.RECENT}/{sourceId}",
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType }
            )
        ) {
            RecentScreen(navController = navController)
        }
        composable(
            route = "${DiscoverRoutes.SEARCH}/{sourceId}?basequery={baseQuery}",
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType },
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
    }
}

object DiscoverRoutes {
    const val RECENT = "recent"
    const val SEARCH = "search"
    const val GLOBAL_SEARCH = "global_search"
}