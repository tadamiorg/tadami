package com.sf.tadami.navigation.graphs.tabs.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

import com.sf.tadami.navigation.graphs.tabs.TabsNavItems
import com.sf.tadami.ui.tabs.library.LibraryScreen

fun NavGraphBuilder.libraryNavGraph(
    navController: NavHostController,
    tabsNavPadding : PaddingValues,
    bottomNavDisplay: Boolean,
    setNavDisplay: (display: Boolean) -> Unit,
    openAnimeDetails : (sourceId : Long,animeId : Long) -> Unit
) {
    composable(
        route = TabsNavItems.Library.route,
    ) {
        LibraryScreen(
            modifier = Modifier.padding(tabsNavPadding),
            navController = navController,
            setNavDisplay = setNavDisplay,
            bottomNavDisplay = bottomNavDisplay,
            openAnimeDetails = openAnimeDetails
        )
    }
}