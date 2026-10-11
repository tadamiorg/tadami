package com.sf.tadami.navigation.graphs.about

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sf.tadami.data.update.GithubUpdate
import com.sf.tadami.ui.tabs.more.about.AboutScreen
import com.sf.tadami.ui.tabs.more.about.NewUpdateScreen

fun NavGraphBuilder.aboutNavGraph(navController: NavHostController) {
    composable(route = AboutRoutes.ABOUT)
    {
        AboutScreen(navHostController = navController)
    }

    composable(route = AboutRoutes.NEW_UPDATE)
    {
        val release = remember { pendingRelease }
        // Pops this screen only, so a second tap while it's leaving doesn't pop the one below
        val close: () -> Unit = { navController.popBackStack(AboutRoutes.NEW_UPDATE, inclusive = true) }
        if (release == null) {
            // Restored after the process was killed: the release is gone, nothing to show
            LaunchedEffect(Unit) { close() }
        } else {
            NewUpdateScreen(release = release, onDismiss = close)
        }
    }
}

/** The release the new update screen shows. Its changelog is too long for a route argument. */
private var pendingRelease: GithubUpdate? = null

fun NavController.navigateToNewUpdate(release: GithubUpdate) {
    pendingRelease = release
    navigate(AboutRoutes.NEW_UPDATE) {
        launchSingleTop = true
    }
}

object AboutRoutes {
    const val ABOUT = "about"
    const val NEW_UPDATE = "new_update"
}
