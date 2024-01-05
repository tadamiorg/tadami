package com.sf.tadami.navigation.graphs.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.tabs.history.HistoryScreen

fun NavGraphBuilder.historyNavGraph(
    navController: NavHostController,
    tabsNavPadding : PaddingValues,
) {

    composable(
        route = HomeNavItems.History.route,
    ) {
        HistoryScreen(
            modifier = Modifier.padding(tabsNavPadding),
            navController = navController,
        )
    }

}