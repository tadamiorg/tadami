package com.sf.tadami.navigation.graphs.animeInfos

import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.Injekt
import com.sf.tadami.ui.PlatformUi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sf.tadami.ui.animeinfos.details.DetailsScreen

fun NavGraphBuilder.animeInfosNavGraph(navController: NavHostController) {
    composable(
        route = "${AnimeInfosRoutes.DETAILS}/{sourceId}/{animeId}?migrationId={migrationId}",
        arguments = listOf(
            navArgument("sourceId") { type = NavType.LongType },
            navArgument("animeId") { type = NavType.LongType },
            navArgument("migrationId") {
                nullable = true
                defaultValue = null
            }
        ),
    ) {
        DetailsScreen(navController)
    }

    with(Injekt.get<PlatformUi>()) {
        webViewDestination("${AnimeInfosRoutes.WEBVIEW}/{sourceId}/{title_key}/{url_key}", navController)
    }

    with(Injekt.get<PlatformUi>()) {
        episodeDestination("${AnimeInfosRoutes.EPISODE}/{sourceId}/{episode}", navController)
    }

}

object AnimeInfosRoutes {
    const val EPISODE = "episode"
    const val DETAILS = "details"
    const val WEBVIEW = "details_webview"
}