package com.sf.animescraper.ui.main

import android.os.Bundle
import android.transition.Explode
import android.transition.Slide
import android.view.Gravity
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.sf.animescraper.navigation.graphs.RootNavGraph
import com.sf.animescraper.ui.themes.AnimeScraperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {
            AnimeScraperTheme {
                RootNavGraph(navController = rememberNavController())
            }
        }
    }
}