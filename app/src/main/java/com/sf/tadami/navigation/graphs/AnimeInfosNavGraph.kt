package com.sf.tadami.navigation.graphs

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.activity
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.sf.tadami.ui.animeinfos.details.DetailsScreen
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity

fun NavGraphBuilder.animeInfosNavGraph(navController: NavHostController) {
    navigation(
        route = AnimeInfosRoutes.INFOS_GRAPH,
        startDestination = "${AnimeInfosRoutes.DETAILS}/{sourceId}/{animeId}"
    ) {

        composable(
            route = "${AnimeInfosRoutes.DETAILS}/{sourceId}/{animeId}",
            arguments = listOf(
                navArgument("sourceId") { type = NavType.StringType },
                navArgument("animeId") { type = NavType.LongType }
            )
        ) {
            DetailsScreen(navController)
        }

        activity("${AnimeInfosRoutes.EPISODE}/{sourceId}/{episode}") {
            this.activityClass = EpisodeActivity::class
            argument("sourceId") {
                this.nullable = false
                this.type = NavType.StringType
            }
            argument("episode") {
                this.nullable = false
                this.type = NavType.LongType
            }
        }
    }
}

object AnimeInfosRoutes {
    const val EPISODE = "episode"
    const val DETAILS = "details"
    const val INFOS_GRAPH = "infos_graph"
}