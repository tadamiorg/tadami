package com.sf.tadami.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sf.tadami.desktop.browser.WebViewScreen
import com.sf.tadami.desktop.cast.CastButton as DesktopCastButton
import com.sf.tadami.desktop.player.EpisodePlayerScreen
import com.sf.tadami.source.online.AnimeHttpSource
import com.sf.tadami.ui.PlatformUi
import com.sf.tadami.ui.tabs.browse.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/** The desktop UI: the player and the WebView are screens of the window, the pickers Swing's, the cast our own. */
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

    /** The embedded browser, as the phone of the app's user agent (or the source's). */
    override fun NavGraphBuilder.webViewDestination(route: String, navController: NavHostController) {
        composable(
            route = route,
            arguments = listOf(
                navArgument("sourceId") { type = NavType.LongType },
                navArgument("title_key") { type = NavType.StringType },
                navArgument("url_key") { type = NavType.StringType },
            ),
        ) { entry ->
            val url = entry.savedStateHandle.get<String>("url_key") ?: return@composable
            val sourceId = entry.savedStateHandle.get<Long>("sourceId")
            // Must match what OkHttp sends: Cloudflare binds its clearance to the user agent
            val userAgent = remember(sourceId) {
                sourceId?.let { runCatching { (Injekt.get<SourceManager>().get(it) as? AnimeHttpSource)?.headers?.get("User-Agent") }.getOrNull() }
            }
            WebViewScreen(
                url = url,
                title = entry.savedStateHandle.get<String>("title_key").orEmpty(),
                userAgent = userAgent,
                onBack = { navController.popBackStack() },
            )
        }
    }

    override val hasCastButton: Boolean get() = true

    @Composable
    override fun CastButton(tint: Color, modifier: Modifier) = DesktopCastButton(tint, modifier)

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
