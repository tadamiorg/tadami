package com.sf.tadami.navigation.graphs.tabs.updates

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.tabs.TabsNavItems
import com.sf.tadami.ui.tabs.updates.UpdatesScreen

fun NavGraphBuilder.updatesNavGraph(
    navController: NavHostController,
    tabsNavPadding : PaddingValues,
    bottomNavDisplay: Boolean,
    setNavDisplay: (display: Boolean) -> Unit,
) {

    composable(
        route = TabsNavItems.Updates.route,
    ) {
        UpdatesScreen(
            modifier = Modifier.padding(tabsNavPadding),
            navController = navController,
            setNavDisplay = setNavDisplay,
            bottomNavDisplay = bottomNavDisplay,
        )
    }

}