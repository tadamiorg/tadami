package com.sf.tadami.navigation.graphs.more

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.tabs.more.MoreScreen
import com.sf.tadami.ui.tabs.more.settings.SettingsScreen

fun NavGraphBuilder.moreNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    composable(route = HomeNavItems.More.route) {
        MoreScreen(
            navHostController = navController,
            modifier = modifier
        )
    }
    composable(
        route = MORE_ROUTES.SETTINGS,
    ) {
        SettingsScreen(
            navController = navController
        )
    }
}

object MORE_ROUTES {
    const val SETTINGS = "settings"
}

