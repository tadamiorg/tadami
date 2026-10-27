package com.sf.tadami.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.sf.tadami.AppBuildInfo
import com.sf.tadami.BackgroundJobs
import com.sf.tadami.extension.api.ExtensionsApi
import com.sf.tadami.navigation.HomeScreen
import com.sf.tadami.navigation.graphs.onboarding.OnboardingRoutes
import com.sf.tadami.preferences.app.BasePreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.ic_launcher
import com.sf.tadami.ui.main.AppUpdaterScreen
import com.sf.tadami.ui.themes.TadamiTheme
import com.sf.tadami.utils.Log
import com.sf.tadami.utils.coil.tadamiDefaults
import com.sf.tadami.utils.editPreference
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toOkioPath
import org.jetbrains.compose.resources.painterResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import javax.swing.UIManager

fun main() {
    AppBuildInfo.APPLICATION_ID = "com.sf.tadami.desktop"
    AppBuildInfo.VERSION_NAME = DESKTOP_VERSION
    AppBuildInfo.RELEASE_VERSION_NAME = DESKTOP_VERSION
    Toasts.install()
    // The file pickers look like the system's
    runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }

    val platformUi = DesktopPlatformUi()
    DesktopModule.install(platformUi)
    // No scheduler outside of the app: the updates run while it's open
    Injekt.get<BackgroundJobs>().scheduleLibraryUpdates()
    Injekt.get<BackgroundJobs>().scheduleAutoBackups()

    application {
        platformUi.closeApp = ::exitApplication
        DesktopWindow.state = rememberWindowState(width = 1280.dp, height = 820.dp)
        Window(
            onCloseRequest = ::exitApplication,
            title = "Tadami",
            icon = painterResource(Res.drawable.ic_launcher),
            state = DesktopWindow.state,
            onPreviewKeyEvent = { DesktopWindow.keyHandler?.invoke(it) ?: false },
        ) {
            LaunchedEffect(Unit) {
                DesktopWindow.window = window
                DevAutomation.start { window }
            }
            setSingletonImageLoaderFactory { context ->
                ImageLoader.Builder(context)
                    .tadamiDefaults(
                        context = context,
                        network = Injekt.get(),
                        diskCacheDir = File(AppDirs.cache, "image_cache").toOkioPath(),
                        crossfadeMillis = 300,
                    )
                    .build()
            }
            TadamiTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) {
                        App()
                        Toasts.Host()
                    }
                }
            }
        }
    }
}

/** What MainActivity shows: the home screen, the onboarding the first time, the update checks. */
@Composable
private fun App() {
    val basePreferences by rememberDataStoreState(BasePreferences).value.collectAsState()
    val navController = rememberNavController()

    AppUpdaterScreen(navController)
    ExtensionsCheckForUpdates()
    HomeScreen(navController, downloadedOnly = basePreferences.downloadedOnly)

    LaunchedEffect(navController) {
        // Once the home screen has set the graph: its effects run after this one on the desktop
        navController.currentBackStackEntryFlow.first()
        if (!basePreferences.onboardingComplete) {
            navController.navigate(OnboardingRoutes.ONBOARDING)
        }
    }
}

@Composable
private fun ExtensionsCheckForUpdates() {
    LaunchedEffect(Unit) {
        try {
            val extsUpdates = ExtensionsApi().checkForUpdates()
            // null when the check was skipped (throttled), keep the current count then
            if (extsUpdates != null) {
                Injekt.get<DataStore<Preferences>>().editPreference(extsUpdates.size, SourcesPreferences.EXT_UPDATES_COUNT)
            }
        } catch (e: Exception) {
            Log.e("ExtensionsCheckForUpdates", e.stackTraceToString())
        }
    }
}

private const val DESKTOP_VERSION = "1.0.0"
