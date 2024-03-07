package com.sf.tadami.navigation.graphs.app.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.ui.tabs.settings.screens.appearance.AppearancePreferencesScreen
import com.sf.tadami.ui.tabs.settings.screens.backup.BackupPreferencesScreen
import com.sf.tadami.ui.tabs.settings.screens.library.LibraryPreferencesScreen
import com.sf.tadami.ui.tabs.settings.screens.player.PlayerPreferencesScreen

fun NavGraphBuilder.nestedSettingsNavGraph(
    navController: NavHostController
) {
    composable(route = SettingsRoutes.LIBRARY) {
        LibraryPreferencesScreen(navController).Content()
    }
    composable(route = SettingsRoutes.PLAYER) {
        PlayerPreferencesScreen(navController).Content()
    }
    composable(route = SettingsRoutes.BACKUP) {
        BackupPreferencesScreen(navController).Content()
    }
    composable(route = SettingsRoutes.APPEARANCE) {
        AppearancePreferencesScreen(navController).Content()
    }
    advancedNavGraph(navController)
}
object SettingsRoutes {
    const val LIBRARY = "library_settings"
    const val PLAYER = "player_settings"
    const val BACKUP = "backup_settings"
    const val APPEARANCE = "appearance_settings"
}