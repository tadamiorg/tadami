package com.sf.animescraper.navigation.graphs

import androidx.navigation.*
import androidx.navigation.compose.composable
import com.sf.animescraper.ui.animeinfos.details.DetailsScreen
import com.sf.animescraper.ui.animeinfos.episode.EpisodeActivity

fun NavGraphBuilder.animeInfosNavGraph(navController: NavHostController) {
    navigation(
        route = "InfosRoutes",
        startDestination = AnimeInfosRoutes.DETAILS
    ){

        composable(route = AnimeInfosRoutes.DETAILS) {
            DetailsScreen(navController)
        }

        activity("${AnimeInfosRoutes.EPISODE}/{title}/{initialEpisode}?episodes={episodes}"){
            this.activityClass = EpisodeActivity::class
            argument("initialEpisode"){
                this.defaultValue = 0
                this.type = NavType.IntType
            }
            argument("title"){
                this.defaultValue = ""
                this.type = NavType.StringType
            }
            argument("episodes"){
                this.defaultValue = "[]"
                this.type = NavType.StringType
            }
        }
    }
}
object AnimeInfosRoutes {
    const val EPISODE = "episode"
    const val DETAILS = "details"
}