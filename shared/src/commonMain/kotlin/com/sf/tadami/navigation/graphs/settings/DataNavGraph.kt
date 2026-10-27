package com.sf.tadami.navigation.graphs.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sf.tadami.ui.tabs.more.settings.screens.data.DataPreferencesScreen
import com.sf.tadami.ui.tabs.more.settings.screens.data.backup.CreateBackupScreen
import com.sf.tadami.ui.tabs.more.settings.screens.data.backup.RestoreBackupScreen

fun NavGraphBuilder.dataNavGraph(navController: NavHostController) {

    composable(
        route = DataSettingsRoutes.HOME,
    ) {
        DataPreferencesScreen(navController).Content()
    }

    composable(
        route = DataSettingsRoutes.CREATE_BACKUP,
    ) {
        CreateBackupScreen(navController).Content()
    }

    composable(
        route = "${DataSettingsRoutes.RESTORE_BACKUP}/{uri}",
        arguments = listOf(
            navArgument("uri") { type = NavType.StringType }
        )
    ) {
        RestoreBackupScreen(navController).Content()
    }

}

object DataSettingsRoutes {
    const val HOME = "data_settings"
    const val CREATE_BACKUP = "data_create_backup"
    const val RESTORE_BACKUP = "data_restore_backup"
}