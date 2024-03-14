package com.sf.tadami.navigation.graphs.tabs.sources

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.tabs.TabsNavItems
import com.sf.tadami.ui.tabs.browse.BrowseScreen

fun NavGraphBuilder.sourcesNavGraph(
    navController: NavHostController,
    tabsNavPadding : PaddingValues,
    openSourceSearch : (sourceId : Long) -> Unit
) {
    composable(
        route = TabsNavItems.Browse.route,
    ) {
        BrowseScreen(
            modifier = Modifier.padding(tabsNavPadding),
            navController = navController,
            openSourceSearch = openSourceSearch
        )
    }
}