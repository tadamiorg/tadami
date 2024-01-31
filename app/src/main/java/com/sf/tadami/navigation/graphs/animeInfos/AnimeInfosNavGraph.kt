package com.sf.tadami.navigation.graphs.animeInfos

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.activity
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sf.tadami.ui.animeinfos.details.DetailsScreen
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity
import com.sf.tadami.ui.webview.WebViewActivity

fun NavGraphBuilder.animeInfosNavGraph(navController: NavHostController) {
    composable(
        route = "${AnimeInfosRoutes.DETAILS}/{sourceId}/{animeId}",
        arguments = listOf(
            navArgument("sourceId") { type = NavType.LongType },
            navArgument("animeId") { type = NavType.LongType }
        )
    ) {
        DetailsScreen(navController)
    }

    activity("${AnimeInfosRoutes.WEBVIEW}/{sourceId}/{title_key}/{url_key}") {
        this.activityClass = WebViewActivity::class
        argument("sourceId") {
            this.nullable = false
            this.type = NavType.LongType
        }
        argument("title_key") {
            this.nullable = false
            this.type = NavType.StringType
        }
        argument("url_key") {
            this.nullable = false
            this.type = NavType.StringType
        }
    }

    activity("${AnimeInfosRoutes.EPISODE}/{sourceId}/{episode}") {
        this.activityClass = EpisodeActivity::class
        argument("sourceId") {
            this.nullable = false
            this.type = NavType.LongType
        }
        argument("episode") {
            this.nullable = false
            this.type = NavType.LongType
        }
    }

}

object AnimeInfosRoutes {
    const val EPISODE = "episode"
    const val DETAILS = "details"
    const val WEBVIEW = "details_webview"
}