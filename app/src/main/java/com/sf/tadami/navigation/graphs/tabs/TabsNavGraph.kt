package com.sf.tadami.navigation.graphs.tabs

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.app.animeInfos.AnimeInfosRoutes
import com.sf.tadami.navigation.graphs.tabs.history.historyNavGraph
import com.sf.tadami.navigation.graphs.tabs.library.libraryNavGraph
import com.sf.tadami.navigation.graphs.tabs.settings.settingsNavGraph
import com.sf.tadami.navigation.graphs.tabs.sources.sourcesNavGraph
import com.sf.tadami.navigation.graphs.tabs.updates.updatesNavGraph

@Composable
fun TabsNavGraph(
    navController: NavHostController,
    tabsNavPadding: PaddingValues,
    setNavDisplay: (display: Boolean) -> Unit,
    libraryFocusedAnime : Long,
    setLibraryFocusedAnime : (animeId : Long) -> Unit,
    openAnimeDetails : (sourceId : Long,animeId : Long) -> Unit,
    openSourceSearch : (sourceId : Long) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = TabsNavItems.Library.route
    ) {

        /* Home Tabs */
        libraryNavGraph(
            navController = navController,
            tabsNavPadding = tabsNavPadding,
            setNavDisplay = setNavDisplay,
            bottomNavDisplay = false,
            libraryFocusedAnime = libraryFocusedAnime,
            openAnimeDetails = openAnimeDetails,
            setLibraryFocusedAnime = setLibraryFocusedAnime
        )

        updatesNavGraph(
            navController = navController,
            tabsNavPadding = tabsNavPadding,
            setNavDisplay = setNavDisplay,
            bottomNavDisplay = false,
        )

        historyNavGraph(
            navController = navController,
            tabsNavPadding = tabsNavPadding,
        )

        sourcesNavGraph(
            navController = navController,
            tabsNavPadding = tabsNavPadding,
            openSourceSearch = openSourceSearch
        )
        settingsNavGraph(
            navController = navController,
            tabsNavPadding = tabsNavPadding,
        )
    }
}

object GRAPH {
    const val TABS = "tabs_graph"
}

sealed class TabsNavItems(val route: String, @StringRes val name: Int, @DrawableRes val icon: Int) {
    data object Library :
        TabsNavItems("library", R.string.library_tab_title, R.drawable.anim_video_library_enter)

    data object Updates :
        TabsNavItems("updates", R.string.label_recent_updates, R.drawable.anim_updates_enter)

    data object History :
        TabsNavItems("history", R.string.label_history, R.drawable.anim_history_enter)

    data object Browse :
        TabsNavItems("anime_sources", R.string.browse_tab_title, R.drawable.anim_sources_enter)

    data object Settings :
        TabsNavItems("settings", R.string.label_more, R.drawable.anim_settings_enter)
}

