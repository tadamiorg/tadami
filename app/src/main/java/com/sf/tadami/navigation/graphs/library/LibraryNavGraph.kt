package com.sf.tadami.navigation.graphs.library

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.tabs.library.LibraryScreen

fun NavGraphBuilder.libraryNavGraph(
    navController: NavHostController,
    setNavDisplay: (display: Boolean) -> Unit,
) {
    composable(
        route = HomeNavItems.Library.route
    ) {
        LibraryScreen(
            navController = navController,
            setNavDisplay = setNavDisplay,
        )
    }
}

object LibraryRoutes {

}