package com.sf.tadami.navigation.graphs.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.ui.tabs.settings.screens.advanced.AdvancedPreferencesScreen
import com.sf.tadami.ui.tabs.settings.screens.advanced.background.WorkerInfosScreen

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
}

object AdvancedSettingsRoutes {
    const val HOME = "advanced_settings"
    const val PROCESS_INFOS = "advanced_process_infos"
}