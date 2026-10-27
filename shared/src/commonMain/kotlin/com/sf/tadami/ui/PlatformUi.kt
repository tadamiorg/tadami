package com.sf.tadami.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder

/**
 * The parts of the UI each platform implements on its own, registered in Injekt: on Android the
 * player and the browser are activities, on the desktop screens of the window.
 */
interface PlatformUi {

    /** The player of an episode, at [route] with its `sourceId` and `episode` (its ID) arguments. */
    fun NavGraphBuilder.episodeDestination(route: String)

    /** The in-app browser, at [route] with its `sourceId`, `title_key` and `url_key` arguments. */
    fun NavGraphBuilder.webViewDestination(route: String)

    /** Whether there's a cast button to show, see [CastButton]. */
    val hasCastButton: Boolean get() = false

    /** The button to cast to a TV, see [hasCastButton]. */
    @Composable
    fun CastButton(tint: Color) = Unit
}
