package com.sf.tadami.navigation.graphs

import androidx.navigation.*
import androidx.navigation.compose.composable
import com.sf.tadami.ui.tabs.settings.screens.library.LibraryPreferencesScreen

fun NavGraphBuilder.settingsNavGraph(navController: NavHostController) {

    composable(route = SettingsRoutes.LIBRARY) {
        LibraryPreferencesScreen(navController).Content()
    }
    composable(route = SettingsRoutes.PLAYER) {

    }
    composable(route = SettingsRoutes.BACKUP) {

    }
}
object SettingsRoutes {
    const val LIBRARY = "library_settings"
    const val PLAYER = "player_settings"
    const val BACKUP = "backup_settings"
}