package com.sf.tadami.navigation.graphs.app.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.ui.tabs.settings.screens.advanced.AdvancedPreferencesScreen
import com.sf.tadami.ui.tabs.settings.screens.advanced.background.WorkerInfosScreen
import com.sf.tadami.ui.tabs.settings.screens.advanced.data.ClearDatabaseScreen

fun NavGraphBuilder.advancedNavGraph(navController: NavHostController) {

    composable(
        route = AdvancedSettingsRoutes.HOME,
    ) {
        AdvancedPreferencesScreen(navController).Content()
    }
    composable(
        route = AdvancedSettingsRoutes.PROCESS_INFOS,
    ) {
        WorkerInfosScreen(navController).Content()
    }

    composable(
        route = AdvancedSettingsRoutes.CLEAR_DATABASE,
    ) {
        ClearDatabaseScreen(navController).Content()
    }
}

object AdvancedSettingsRoutes {
    const val HOME = "advanced_settings"
    const val PROCESS_INFOS = "advanced_process_infos"
    const val CLEAR_DATABASE = "advanced_clear_database"
}