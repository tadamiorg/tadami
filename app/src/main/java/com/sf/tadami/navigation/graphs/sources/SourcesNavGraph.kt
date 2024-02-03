package com.sf.tadami.navigation.graphs.sources

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.source.ConfigurableParsedHttpAnimeSource
import com.sf.tadami.ui.tabs.browse.BrowseScreen
import com.sf.tadami.ui.tabs.browse.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun NavGraphBuilder.sourcesNavGraph(
    navController: NavHostController,
    tabsNavPadding : PaddingValues
) {
    composable(
        route = HomeNavItems.Sources.route,
    ) {
        BrowseScreen(
            modifier = Modifier.padding(tabsNavPadding),
            navController = navController
        )
    }
    composable(
        route = "${SourcesRoutes.SETTINGS}/{sourceId}",
        arguments = listOf(
            navArgument("sourceId") { type = NavType.LongType }
        )
    ) {
        val sourcesManager: SourceManager = Injekt.get()
        val source =
            sourcesManager.getOrStub(it.arguments?.getLong("sourceId")!!) as ConfigurableParsedHttpAnimeSource<*>
        source.getPreferenceScreen(navController).Content()
    }
}

object SourcesRoutes {
    const val SETTINGS = "sources_settings"
}