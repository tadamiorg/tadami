package com.sf.tadami.navigation.graphs.history

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.tabs.history.HistoryScreen

fun NavGraphBuilder.historyNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {

    composable(
        route = HomeNavItems.History.route,
    ) {
        HistoryScreen(
            modifier = modifier,
            navController = navController,
        )
    }
}