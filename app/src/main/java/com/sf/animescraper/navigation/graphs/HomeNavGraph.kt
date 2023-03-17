package com.sf.animescraper.navigation.graphs

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sf.animescraper.R
import com.sf.animescraper.ui.tabs.animesources.AnimeSourcesScreen
import com.sf.animescraper.ui.tabs.favorites.FavoritesScreen
import com.sf.animescraper.ui.tabs.settings.SettingsScreen

@Composable
fun HomeNavGraph(
    navController: NavHostController,
    bottomNavDisplay : Boolean,
    setNavDisplay : (display : Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        route = GRAPH.HOME,
        startDestination = HomeNavItems.Favorites.route
    ) {
        composable(route = HomeNavItems.Favorites.route){
            FavoritesScreen(
                navController = navController,
                setNavDisplay = setNavDisplay,
                bottomNavDisplay = bottomNavDisplay
            )

        }
        composable(route = HomeNavItems.Sources.route){
            AnimeSourcesScreen(navController = navController)
        }
        composable(route = HomeNavItems.Settings.route){
            SettingsScreen()
        }
        discoverNavGraph(navController)
        animeInfosNavGraph(navController)

    }
}


sealed class HomeNavItems(val route: String, @StringRes val name: Int,@DrawableRes val icon: Int) {
    object Favorites : HomeNavItems("favorite", R.string.favorites_tab_title,R.drawable.ic_favorites)
    object Sources : HomeNavItems("anime_sources", R.string.sources_tab_title,R.drawable.ic_sources)
    object Settings : HomeNavItems("settings", R.string.settings_tab_title,R.drawable.ic_settings)
}

