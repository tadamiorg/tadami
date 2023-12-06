package com.sf.tadami.navigation.graphs.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.animeInfos.animeInfosNavGraph
import com.sf.tadami.navigation.graphs.discover.discoverNavGraph
import com.sf.tadami.navigation.graphs.library.libraryNavGraph
import com.sf.tadami.navigation.graphs.settings.settingsNavGraph
import com.sf.tadami.navigation.graphs.sources.sourcesNavGraph

@Composable
fun HomeNavGraph(
    navController: NavHostController,
    bottomNavDisplay: Boolean,
    setNavDisplay: (display: Boolean) -> Unit,
    librarySheetVisible: Boolean,
    showLibrarySheet: () -> Unit,
) {
    NavHost(
        navController = navController,
        route = GRAPH.HOME,
        startDestination = HomeNavItems.Library.route
    ) {

        /* Home Tabs */
        libraryNavGraph(
            navController = navController,
            setNavDisplay = setNavDisplay,
            bottomNavDisplay = bottomNavDisplay,
            librarySheetVisible = librarySheetVisible,
            showLibrarySheet = showLibrarySheet
        )

        sourcesNavGraph(navController)
        settingsNavGraph(navController)

        /* Nested navigation */
        discoverNavGraph(navController)
        animeInfosNavGraph(navController)
    }
}

object GRAPH {
    const val HOME = "home_graph"
}

sealed class HomeNavItems(val route: String, @StringRes val name: Int, @DrawableRes val icon: Int) {
    object Library :
        HomeNavItems("library", R.string.library_tab_title, R.drawable.ic_video_library)

    object Sources :
        HomeNavItems("anime_sources", R.string.sources_tab_title, R.drawable.ic_sources)

    object Settings : HomeNavItems("settings", R.string.settings_tab_title, R.drawable.ic_settings)
}

