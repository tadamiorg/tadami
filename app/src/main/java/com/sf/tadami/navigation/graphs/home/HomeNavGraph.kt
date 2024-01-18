package com.sf.tadami.navigation.graphs.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.animeInfos.animeInfosNavGraph
import com.sf.tadami.navigation.graphs.discover.discoverNavGraph
import com.sf.tadami.navigation.graphs.history.historyNavGraph
import com.sf.tadami.navigation.graphs.library.libraryNavGraph
import com.sf.tadami.navigation.graphs.settings.settingsNavGraph
import com.sf.tadami.navigation.graphs.sources.sourcesNavGraph
import com.sf.tadami.navigation.graphs.updates.updatesNavGraph

@Composable
fun HomeNavGraph(
    navController: NavHostController,
    tabsNavPadding: PaddingValues,
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
            tabsNavPadding = tabsNavPadding,
            setNavDisplay = setNavDisplay,
            bottomNavDisplay = bottomNavDisplay,
            librarySheetVisible = librarySheetVisible,
            showLibrarySheet = showLibrarySheet
        )

        updatesNavGraph(
            navController = navController,
            tabsNavPadding = tabsNavPadding,
            setNavDisplay = setNavDisplay,
            bottomNavDisplay = bottomNavDisplay,
        )

        historyNavGraph(
            navController = navController,
            tabsNavPadding = tabsNavPadding,
        )

        sourcesNavGraph(
            navController = navController,
            tabsNavPadding = tabsNavPadding,
        )
        settingsNavGraph(
            navController = navController,
            tabsNavPadding = tabsNavPadding,
        )

        /* Nested navigation */
        discoverNavGraph(navController)
        animeInfosNavGraph(navController)
    }
}

object GRAPH {
    const val HOME = "home_graph"
}

sealed class HomeNavItems(val route: String, @StringRes val name: Int, @DrawableRes val icon: Int) {
    data object Library :
        HomeNavItems("library", R.string.library_tab_title, R.drawable.anim_video_library_enter)

    data object Updates :
        HomeNavItems("updates", R.string.label_recent_updates, R.drawable.anim_updates_enter)

    data object History :
        HomeNavItems("history", R.string.label_history, R.drawable.anim_history_enter)

    data object Sources :
        HomeNavItems("anime_sources", R.string.sources_tab_title, R.drawable.anim_sources_enter)

    data object Settings :
        HomeNavItems("settings", R.string.label_more, R.drawable.anim_settings_enter)
}

