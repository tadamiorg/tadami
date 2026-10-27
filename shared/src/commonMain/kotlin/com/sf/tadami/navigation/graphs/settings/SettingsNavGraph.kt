package com.sf.tadami.navigation.graphs.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.ui.category.CategoryScreen
import com.sf.tadami.ui.tabs.more.settings.screens.appearance.AppearancePreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.screens.download.DownloadPreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.screens.library.LibraryPreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.screens.player.PlayerPreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.search.SettingsSearchScreen

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController
) {
    composable(route = SettingsRoutes.LIBRARY) {
        LibraryPreferencesScreen(navController).Content()
    }
    composable(route = SettingsRoutes.CATEGORIES) {
        CategoryScreen(navController)
    }
    composable(route = SettingsRoutes.PLAYER) {
        PlayerPreferencesScreen(navController).Content()
    }
    composable(route = SettingsRoutes.APPEARANCE) {
        AppearancePreferencesScreen(navController).Content()
    }
    composable(route = SettingsRoutes.DOWNLOADS) {
        DownloadPreferencesScreen(navController).Content()
    }
    composable(route = SettingsRoutes.SEARCH) {
        SettingsSearchScreen(navController)
    }
    advancedNavGraph(navController)
    dataNavGraph(navController)
}
object SettingsRoutes {
    const val LIBRARY = "library_settings"
    const val CATEGORIES = "categories"
    const val PLAYER = "player_settings"
    const val APPEARANCE = "appearance_settings"
    const val DOWNLOADS = "download_settings"
    const val SEARCH = "settings_search"
}