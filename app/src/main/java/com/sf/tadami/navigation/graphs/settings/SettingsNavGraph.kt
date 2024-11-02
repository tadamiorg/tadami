package com.sf.tadami.navigation.graphs.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.ui.tabs.more.settings.screens.appearance.AppearancePreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.screens.data.DataPreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.screens.library.LibraryPreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.screens.player.PlayerPreferencesScreen

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController
) {
    composable(route = SettingsRoutes.LIBRARY) {
        LibraryPreferencesScreen(navController).Content()
    }
    composable(route = SettingsRoutes.PLAYER) {
        PlayerPreferencesScreen(navController).Content()
    }
    composable(route = SettingsRoutes.DATA) {
        DataPreferencesScreen(navController).Content()
    }
    composable(route = SettingsRoutes.APPEARANCE) {
        AppearancePreferencesScreen(navController).Content()
    }
    advancedNavGraph(navController)
}
object SettingsRoutes {
    const val LIBRARY = "library_settings"
    const val PLAYER = "player_settings"
    const val DATA = "data_settings"
    const val APPEARANCE = "appearance_settings"
}