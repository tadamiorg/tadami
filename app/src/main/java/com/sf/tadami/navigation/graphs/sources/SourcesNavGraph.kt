package com.sf.tadami.navigation.graphs.sources

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.activity
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.ui.tabs.browse.BrowseScreen
import com.sf.tadami.ui.tabs.browse.tabs.sources.preferences.SourcePreferencesScreen
import com.sf.tadami.ui.webview.WebViewActivity

fun NavGraphBuilder.sourcesNavGraph(
    navController: NavHostController,
    tabsNavPadding : PaddingValues
) {
    composable(
        route = HomeNavItems.Browse.route,
    ) {
        BrowseScreen(
            modifier = Modifier.padding(tabsNavPadding),
            navController = navController
        )
    }

    activity("${SourcesRoutes.EXTENSIONS_WEBVIEW}/{sourceId}/{title_key}/{url_key}") {
        this.activityClass = WebViewActivity::class
        argument("sourceId") {
            this.nullable = false
            this.type = NavType.LongType
        }
        argument("title_key") {
            this.nullable = false
            this.type = NavType.StringType
        }
        argument("url_key") {
            this.nullable = false
            this.type = NavType.StringType
        }
    }

    composable(
        route = "${SourcesRoutes.SETTINGS}/{sourceId}",
        arguments = listOf(
            navArgument("sourceId") { type = NavType.LongType }
        )
    ) {
        SourcePreferencesScreen(navController = navController)
    }
}

object SourcesRoutes {
    const val SETTINGS = "sources_settings"
    const val EXTENSIONS_WEBVIEW = "extensions_webview"
}