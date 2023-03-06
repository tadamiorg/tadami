package com.sf.animescraper.navigation.graphs

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sf.animescraper.navigation.HomeScreen

@Composable
fun RootNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        route = GRAPH.ROOT,
        startDestination = GRAPH.HOME
    ) {
        composable(GRAPH.HOME){
            HomeScreen()
        }
    }
}

object GRAPH {
    const val HOME = "home_graph"
    const val ROOT = "root_graph"
}