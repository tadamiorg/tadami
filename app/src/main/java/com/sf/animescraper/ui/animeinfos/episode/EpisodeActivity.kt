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
import com.sf.animescraper.network.scraping.dto.details.DetailsEpisode
import com.sf.animescraper.ui.themes.AnimeScraperTheme
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class EpisodeActivity : ComponentActivity() {

    private val json: Json = Injekt.get()

    override fun onCreate(savedInstanceState: Bundle?) {

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE

        super.onCreate(savedInstanceState)

        val extras = intent.extras

        val initialEpisode = extras?.getInt("initialEpisode")

        val title = extras?.getString("title")

        val episodesExtra = extras?.getString("episodes")

        val parsedEpisodes = json.decodeFromString<List<DetailsEpisode>>(episodesExtra!!)

        setContent {
            AnimeScraperTheme {
                EpisodeNavGraph(
                    navController = rememberNavController(),
                    title = title!!,
                    initialEpisode = initialEpisode!!,
                    episodes = parsedEpisodes
                )
            }
        }

    }
}