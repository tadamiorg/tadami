package com.sf.tadami.ui.animeinfos.episode

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.sf.tadami.navigation.graphs.EpisodeNavGraph
import com.sf.tadami.ui.themes.TadamiTheme

class EpisodeActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        super.onCreate(savedInstanceState)

        val extras = intent.extras

        val initialEpisode = checkNotNull(extras?.getLong("episode"))
        val sourceId = checkNotNull(extras?.getString("sourceId"))

        setContent {
            TadamiTheme {
                EpisodeNavGraph(
                    navController = rememberNavController(),
                    episode = initialEpisode,
                    sourceId = sourceId
                )
            }
        }

    }
}