package com.sf.tadami.navigation.graphs.history

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.tabs.history.HistoryScreen

fun NavGraphBuilder.historyNavGraph(navController: NavHostController) {

    composable(
        route = HomeNavItems.History.route,
    ) {
        HistoryScreen(navController = navController)
    }

}