package com.sf.tadami.navigation.graphs.migrate

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sf.tadami.ui.discover.migrate.MigrateSearchScreen

fun NavGraphBuilder.migrateNavGraph(navController: NavHostController) {
    composable(
        route = "${MigrateRoutes.MIGRATE}/{animeId}",
        arguments = listOf(
            navArgument("animeId") { type = NavType.LongType },
        )
    ) {
        MigrateSearchScreen(navController)
    }



}

object MigrateRoutes {
    const val MIGRATE = "migrate_screen"
}