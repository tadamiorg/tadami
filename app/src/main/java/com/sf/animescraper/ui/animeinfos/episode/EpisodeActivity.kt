package com.sf.animescraper.ui.animeinfos.episode

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.sf.animescraper.navigation.graphs.EpisodeNavGraph
import com.sf.animescraper.ui.themes.AnimeScraperTheme

class EpisodeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE

        super.onCreate(savedInstanceState)

        val extras = intent.extras

        val initialEpisode = checkNotNull(extras?.getLong("episode"))
        val sourceId = checkNotNull(extras?.getString("sourceId"))

        setContent {
            AnimeScraperTheme {
                EpisodeNavGraph(
                    navController = rememberNavController(),
                    episode = initialEpisode,
                    sourceId = sourceId
                )
            }
        }

    }
}