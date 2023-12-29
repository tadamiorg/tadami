package com.sf.tadami.navigation.graphs.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.tabs.settings.SettingsScreen
import com.sf.tadami.ui.tabs.settings.screens.backup.BackupPreferencesScreen
import com.sf.tadami.ui.tabs.settings.screens.library.LibraryPreferencesScreen
import com.sf.tadami.ui.tabs.settings.screens.player.PlayerPreferencesScreen

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController,
    tabsNavPadding : PaddingValues,
) {

    composable(
        route = HomeNavItems.Settings.route,
    ) {
        SettingsScreen(
            modifier = Modifier.padding(tabsNavPadding),
            navController = navController
        )
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