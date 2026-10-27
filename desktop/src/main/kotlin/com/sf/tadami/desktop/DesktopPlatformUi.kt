package com.sf.tadami.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sf.tadami.desktop.player.EpisodePlayerScreen
import com.sf.tadami.utils.openInBrowser
import com.sf.tadami.ui.PlatformUi
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/** The desktop UI: the player is a screen of the window, the browser the user's, the pickers Swing's. */
class DesktopPlatformUi : PlatformUi {

    /** Closes the window, set once the app is running. */
    var closeApp: () -> Unit = {}

    override fun NavGraphBuilder.episodeDestination(route: String, navController: NavHostController) {
        composable(
            route = route,
            arguments = listOf(
                navArgument("sourceId") { type = NavType.LongType },
                navArgument("episode") { type = NavType.LongType },
            ),
        ) {
            EpisodePlayerScreen(onBack = { navController.popBackStack() })
        }
    }

    /** No WebView on the desktop yet: the user's browser opens the page. */
    override fun NavGraphBuilder.webViewDestination(route: String, navController: NavHostController) {
        composable(
            route = route,
            arguments = listOf(
                navArgument("sourceId") { type = NavType.LongType },
                navArgument("title_key") { type = NavType.StringType },
                navArgument("url_key") { type = NavType.StringType },
            ),
        ) { entry ->
            val url = entry.savedStateHandle.get<String>("url_key")
            LaunchedEffect(url) {
                url?.let { openInBrowser(it) }
                navController.popBackStack()
            }
        }
    }

    @Composable
    override fun rememberFolderPicker(onPicked: (uri: String) -> Unit): () -> Unit = {
        val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            onPicked(chooser.selectedFile.toURI().toString())
        }
    }

    @Composable
    override fun rememberFilePicker(title: String, onPicked: (uri: String?) -> Unit): () -> Unit = {
        val chooser = JFileChooser().apply { dialogTitle = title }
        val picked = chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION
        onPicked(if (picked) chooser.selectedFile.toURI().toString() else null)
    }

    @Composable
    override fun rememberFileSaver(mimeType: String, onPicked: (uri: String) -> Unit): (name: String) -> Unit = { name ->
        val chooser = JFileChooser().apply {
            selectedFile = File(name)
            name.substringAfterLast('.', "").takeIf { it.isNotEmpty() }?.let {
                fileFilter = FileNameExtensionFilter(".$it", it)
            }
        }
        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            // The file the URI names must exist, as a picked document does
            file.createNewFile()
            onPicked(file.toURI().toString())
        }
    }

    @Composable
    override fun rememberCloseApp(): () -> Unit = { closeApp() }

    /** The logs are in the console: the app's data folder is where its files are. */
    @Composable
    override fun rememberCrashLogsDump(): suspend () -> Unit = {
        runCatching { Desktop.getDesktop().open(AppDirs.data) }
    }
}
