package com.sf.tadami.navigation.graphs.tabs.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.tabs.TabsNavItems
import com.sf.tadami.ui.tabs.settings.SettingsScreen

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController,
    tabsNavPadding : PaddingValues,
) {
    composable(
        route = TabsNavItems.Settings.route,
    ) {
        SettingsScreen(
            modifier = Modifier.padding(tabsNavPadding),
            navController = navController
        )
    }
}