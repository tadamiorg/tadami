package com.sf.animescraper.navigation.graphs

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sf.animescraper.network.scraping.dto.details.DetailsEpisode
import com.sf.animescraper.ui.animeinfos.episode.player.VideoPlayer

@Composable
fun EpisodeNavGraph(
    navController: NavHostController,
    title: String,
    initialEpisode: Int,
    episodes: List<DetailsEpisode>,
) {
    NavHost(
        navController = navController,
        startDestination = EpisodeNavGraph.SCREEN
    ) {
        composable(EpisodeNavGraph.SCREEN) {
            VideoPlayer(animeTitle = title, episodesList = episodes, initialEpisode = initialEpisode)
        }
    }
}

object EpisodeNavGraph {
    const val SCREEN = "video_player"
}
