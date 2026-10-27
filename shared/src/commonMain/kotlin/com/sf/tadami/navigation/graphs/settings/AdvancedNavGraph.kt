package com.sf.tadami.navigation.graphs.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.ui.PlatformUi
import com.sf.tadami.ui.tabs.more.settings.screens.advanced.AdvancedPreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.screens.advanced.data.ClearDatabaseScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun NavGraphBuilder.advancedNavGraph(navController: NavHostController) {

    composable(
        route = AdvancedSettingsRoutes.HOME,
    ) {
        AdvancedPreferencesScreen(navController).Content()
    }
    with(Injekt.get<PlatformUi>()) { advancedDestinations(navController) }

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