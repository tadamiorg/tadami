package com.sf.tadami.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.sf.tadami.preferences.appearance.ThemeMode
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.ui.main.onboarding.steps.OnboardingStep

/**
 * The parts of the UI each platform implements on its own, registered in Injekt: on Android the
 * player and the browser are activities, on the desktop screens of the window.
 */
interface PlatformUi {

    /**
     * The player of an episode, at [route] with its `sourceId` and `episode` (its ID) arguments, left
     * with [navController].
     */
    fun NavGraphBuilder.episodeDestination(route: String, navController: NavHostController)

    /** The in-app browser, at [route] with its `sourceId`, `title_key` and `url_key` arguments. */
    fun NavGraphBuilder.webViewDestination(route: String, navController: NavHostController)

    /** Whether there's a cast button to show, see [CastButton]. */
    val hasCastButton: Boolean get() = false

    /** The button to cast to a TV, see [hasCastButton]. */
    @Composable
    fun CastButton(tint: Color) = Unit

    /**
     * Asks for a folder the app keeps access to: the returned function opens the picker, [onPicked]
     * gets the folder's URI. Nothing when the user cancels, a toast when it can't be opened or kept.
     */
    @Composable
    fun rememberFolderPicker(onPicked: (uri: String) -> Unit): () -> Unit

    /** Asks for a file to read, [title] over the picker: [onPicked] gets its URI, null when cancelled. */
    @Composable
    fun rememberFilePicker(title: String, onPicked: (uri: String?) -> Unit): () -> Unit

    /**
     * Asks where to save a new file of [mimeType]: the returned function opens the picker with the
     * suggested name, [onPicked] gets the file's URI. Nothing when the user cancels.
     */
    @Composable
    fun rememberFileSaver(mimeType: String, onPicked: (uri: String) -> Unit): (name: String) -> Unit

    /** Whether the system confirms a copy to the clipboard itself, the app saying nothing then. */
    val confirmsCopies: Boolean get() = false

    /** Applies [mode] to what the platform draws itself, outside of the theme. */
    fun applyThemeMode(mode: ThemeMode) = Unit

    /** Closes the app, the back action of its first screen. */
    @Composable
    fun rememberCloseApp(): () -> Unit

    /** The step of the onboarding that asks for the platform permissions, null without one. */
    fun permissionStep(): OnboardingStep? = null

    /** Shares the logs of the last crash, of this session when there's none. */
    @Composable
    fun rememberCrashLogsDump(): suspend () -> Unit

    /** Clears the data of the in-app browser but its cookies, the network's. */
    fun clearWebViewData() = Unit

    /** The groups of the advanced settings only the platform has, after the others. */
    @Composable
    fun advancedPreferenceGroups(navController: NavHostController): List<Preference> = emptyList()

    /** The settings screens of [advancedPreferenceGroups]. */
    fun NavGraphBuilder.advancedDestinations(navController: NavHostController) = Unit
}
