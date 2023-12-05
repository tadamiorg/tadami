package com.sf.tadami.navigation.graphs.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.tabs.settings.SettingsScreen
import com.sf.tadami.ui.tabs.settings.screens.backup.BackupPreferencesScreen
import com.sf.tadami.ui.tabs.settings.screens.library.LibraryPreferencesScreen
import com.sf.tadami.ui.tabs.settings.screens.player.PlayerPreferencesScreen

fun NavGraphBuilder.settingsNavGraph(navController: NavHostController) {

    composable(
        route = HomeNavItems.Settings.route,
    ) {
        SettingsScreen(navController = navController)
    }
    composable(route = SettingsRoutes.LIBRARY) {
        LibraryPreferencesScreen(navController).Content()
    }
    composable(route = SettingsRoutes.PLAYER) {
        PlayerPreferencesScreen(navController).Content()
    }
    composable(route = SettingsRoutes.BACKUP) {
        BackupPreferencesScreen(navController).Content()
    }
    advancedNavGraph(navController)
}
object SettingsRoutes {
    const val LIBRARY = "library_settings"
    const val PLAYER = "player_settings"
    const val BACKUP = "backup_settings"
}