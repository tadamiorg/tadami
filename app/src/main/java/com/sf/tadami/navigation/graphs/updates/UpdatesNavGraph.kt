package com.sf.tadami.navigation.graphs.updates

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.tabs.updates.UpdatesScreen

fun NavGraphBuilder.updatesNavGraph(
    navController: NavHostController,
    tabsNavPadding : PaddingValues,
) {

    composable(
        route = HomeNavItems.Updates.route,
    ) {
        UpdatesScreen(
            modifier = Modifier.padding(tabsNavPadding),
            navController = navController,
        )
    }

}