package com.sf.tadami.navigation.graphs.updates

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.tabs.updates.UpdatesScreen

fun NavGraphBuilder.updatesNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    setNavDisplay: (display: Boolean) -> Unit,
) {

    composable(
        route = HomeNavItems.Updates.route,
    ) {
        UpdatesScreen(
            modifier = modifier,
            navController = navController,
            setNavDisplay = setNavDisplay,
        )
    }

}