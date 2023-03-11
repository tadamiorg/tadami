package com.sf.animescraper.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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