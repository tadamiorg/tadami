package com.sf.tadami.navigation.graphs.sources

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.network.api.online.ConfigurableParsedHttpAnimeSource
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesManager
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun NavGraphBuilder.sourcesNavGraph(
    navController: NavHostController
) {
    composable(
        route = HomeNavItems.Sources.route,
    ) {
        AnimeSourcesScreen(navController = navController)
    }
    composable(
        route = "${SourcesRoutes.SETTINGS}/{sourceId}",
        arguments = listOf(
            navArgument("sourceId") { type = NavType.StringType }
        )
    ) {
        val sourcesManager: AnimeSourcesManager = Injekt.get()
        val source =
            sourcesManager.getExtensionById(it.arguments?.getString("sourceId")) as ConfigurableParsedHttpAnimeSource<*>
        source.getPreferenceScreen(navController).Content()
    }
}

object SourcesRoutes {
    const val SETTINGS = "sources_settings"
}