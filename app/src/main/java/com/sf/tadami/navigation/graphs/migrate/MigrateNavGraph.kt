package com.sf.tadami.navigation.graphs.migrate

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.activity
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sf.tadami.ui.animeinfos.details.DetailsScreen
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity
import com.sf.tadami.ui.discover.migrate.MigrateSearchScreen
import com.sf.tadami.ui.webview.WebViewActivity

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