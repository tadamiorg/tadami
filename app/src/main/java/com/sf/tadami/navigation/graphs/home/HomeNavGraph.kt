package com.sf.tadami.navigation.graphs.home

import com.sf.tadami.ui.components.material.AnimatedIcon
import org.jetbrains.compose.resources.StringResource
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.sf.tadami.R
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.navigation.animations.findEnterAnimation
import com.sf.tadami.navigation.animations.findExitAnimation
import com.sf.tadami.navigation.graphs.about.aboutNavGraph
import com.sf.tadami.navigation.graphs.animeInfos.animeInfosNavGraph
import com.sf.tadami.navigation.graphs.discover.discoverNavGraph
import com.sf.tadami.navigation.graphs.history.historyNavGraph
import com.sf.tadami.navigation.graphs.library.libraryNavGraph
import com.sf.tadami.navigation.graphs.migrate.migrateNavGraph
import com.sf.tadami.navigation.graphs.more.moreNavGraph
import com.sf.tadami.navigation.graphs.onboarding.onboardingNavGraph
import com.sf.tadami.navigation.graphs.settings.settingsNavGraph
import com.sf.tadami.navigation.graphs.sources.sourcesNavGraph
import com.sf.tadami.navigation.graphs.updates.updatesNavGraph
import com.sf.tadami.preferences.app.BasePreferences
import com.sf.tadami.preferences.model.rememberDataStoreState

@Composable
fun HomeNavGraph(
    navController: NavHostController,
    setNavDisplay: (display: Boolean) -> Unit
) {
    val basePreferencesState = rememberDataStoreState(BasePreferences)

    NavHost(
        navController = navController,
        route = GRAPH.HOME,
        startDestination = HomeNavItems.Library.route,
        popExitTransition = {
            findExitAnimation(true)
        },
        popEnterTransition = {
            findEnterAnimation(false)
        },
        enterTransition = {
            findEnterAnimation(true)
        },
        exitTransition = {
            findExitAnimation(false)
        }
    ) {

        /* Onboarding screen */
        onboardingNavGraph(
            navController = navController,
            basePreferencesState = basePreferencesState
        )

        /* Home Tabs */
        libraryNavGraph(
            navController = navController,
            setNavDisplay = setNavDisplay,
        )

        updatesNavGraph(
            navController = navController,
            setNavDisplay = setNavDisplay,
        )

        historyNavGraph(
            navController = navController,
        )

        sourcesNavGraph(
            navController = navController,
        )

        moreNavGraph(
            navController = navController,
        )

        /* Nested navigation */
        settingsNavGraph(navController)
        discoverNavGraph(navController)
        animeInfosNavGraph(navController)
        migrateNavGraph(navController)
        aboutNavGraph(navController)
    }
}

object GRAPH {
    const val HOME = "home_graph"
}

sealed class HomeNavItems(val route: String, val name: StringResource, val icon: AnimatedIcon) {
    data object Library :
        HomeNavItems("library", Res.string.library_tab_title, AnimatedIcon.VIDEO_LIBRARY)

    data object Updates :
        HomeNavItems("updates", Res.string.label_recent_updates, AnimatedIcon.UPDATES)

    data object History :
        HomeNavItems("history", Res.string.label_history, AnimatedIcon.HISTORY)

    data object Browse :
        HomeNavItems("anime_sources", Res.string.browse_tab_title, AnimatedIcon.SOURCES)

    data object More :
        HomeNavItems("more", Res.string.label_more, AnimatedIcon.SETTINGS)

    companion object {
        fun includes(route: String?): Boolean {
            return listOf(Library, Updates, History, Browse, More).any { it.route == route }
        }
    }
}

