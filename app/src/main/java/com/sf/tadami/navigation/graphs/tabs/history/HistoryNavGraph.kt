package com.sf.tadami.navigation.graphs.tabs.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.tabs.TabsNavItems
import com.sf.tadami.ui.tabs.history.HistoryScreen

fun NavGraphBuilder.historyNavGraph(
    navController: NavHostController,
    tabsNavPadding : PaddingValues,
) {

    composable(
        route = TabsNavItems.History.route,
    ) {
        HistoryScreen(
            modifier = Modifier.padding(tabsNavPadding),
            navController = navController,
        )
    }

}