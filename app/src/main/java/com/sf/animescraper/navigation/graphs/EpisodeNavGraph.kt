package com.sf.animescraper.navigation.graphs

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sf.animescraper.ui.animeinfos.episode.player.VideoPlayer

@Composable
fun EpisodeNavGraph(
    navController: NavHostController,
    episode: Long,
    sourceId : String
) {
    NavHost(
        navController = navController,
        startDestination = "${EpisodeNavGraph.SCREEN}/{sourceId}/{episode}"
    ) {
        composable("${EpisodeNavGraph.SCREEN}/{sourceId}/{episode}", arguments = listOf(
            navArgument("episode"){
                defaultValue = episode
                type = NavType.LongType
            },
            navArgument("sourceId"){
                defaultValue = sourceId
                type = NavType.StringType
            }
        )) {
            VideoPlayer()
        }
    }
}

object EpisodeNavGraph {
    const val SCREEN = "video_player"
}
