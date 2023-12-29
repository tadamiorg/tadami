package com.sf.tadami.navigation.graphs.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.tabs.library.LibraryScreen

fun NavGraphBuilder.libraryNavGraph(
    navController: NavHostController,
    tabsNavPadding : PaddingValues,
    bottomNavDisplay: Boolean,
    setNavDisplay: (display: Boolean) -> Unit,
    librarySheetVisible: Boolean,
    showLibrarySheet: () -> Unit,
) {
    composable(
        route = HomeNavItems.Library.route,
    ) {
        LibraryScreen(
            modifier = Modifier.padding(tabsNavPadding),
            navController = navController,
            setNavDisplay = setNavDisplay,
            bottomNavDisplay = bottomNavDisplay,
            librarySheetVisible = librarySheetVisible,
            showLibrarySheet = showLibrarySheet
        )
    }
}

object LibraryRoutes {

}