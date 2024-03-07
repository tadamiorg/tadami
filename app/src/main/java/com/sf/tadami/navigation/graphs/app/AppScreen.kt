package com.sf.tadami.navigation.graphs.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun AppScreen(
    navController : NavHostController
) {
    AppNavGraph(navController = navController)
}