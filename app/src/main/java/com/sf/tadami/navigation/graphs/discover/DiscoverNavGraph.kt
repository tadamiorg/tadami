package com.sf.tadami.navigation.graphs.discover

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sf.tadami.ui.discover.globalSearch.GlobalSearchScreen
import com.sf.tadami.ui.discover.recent.RecentScreen
import com.sf.tadami.ui.discover.search.SearchScreen
import com.sf.tadami.ui.tabs.browse.tabs.extensions.details.ExtensionDetailsScreen
import com.sf.tadami.ui.tabs.browse.tabs.extensions.filters.ExtensionsFilterScreen
import com.sf.tadami.ui.tabs.browse.tabs.sources.filters.SourcesFilterScreen

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
        route = "${DiscoverRoutes.SEARCH}/{sourceId}?initialQuery={initialQuery}&migrationId={migrationId}",
        arguments = listOf(
            navArgument("sourceId") { type = NavType.LongType },
            navArgument("initialQuery") {
                nullable = true
                defaultValue = null
                type = NavType.StringType
            },
            navArgument("migrationId") {
                nullable = true
                defaultValue = null
            }
        )
    ) {
        SearchScreen(
            navController = navController
        )
    }
    composable(route = DiscoverRoutes.GLOBAL_SEARCH)
    {
        GlobalSearchScreen(navController = navController)
    }
    composable(route = DiscoverRoutes.SOURCES_FILTER)
    {
        SourcesFilterScreen(navController = navController)
    }
    composable(route = DiscoverRoutes.EXTENSIONS_FILTER)
    {
        ExtensionsFilterScreen(navController = navController)
    }
    composable(
        route = "${DiscoverRoutes.EXTENSION_DETAILS}/{pkgName}",
        arguments = listOf(
            navArgument("pkgName") { type = NavType.StringType },
        )
    )
    {
        ExtensionDetailsScreen(navController = navController)
    }
}

object DiscoverRoutes {
    const val RECENT = "recent"
    const val SEARCH = "search"
    const val GLOBAL_SEARCH = "global_search"
    const val SOURCES_FILTER = "sources_filter"
    const val EXTENSIONS_FILTER = "extensions_filter"
    const val EXTENSION_DETAILS = "extension_details"
}