package com.sf.tadami.navigation.graphs.about

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.ui.tabs.more.about.AboutScreen

fun NavGraphBuilder.aboutNavGraph(navController: NavHostController) {
    composable(route = AboutRoutes.ABOUT)
    {
        AboutScreen(navHostController = navController)
    }
}

object AboutRoutes {
    const val ABOUT = "about"
}