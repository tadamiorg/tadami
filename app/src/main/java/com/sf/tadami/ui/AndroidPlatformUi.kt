package com.sf.tadami.ui

import androidx.compose.ui.Modifier
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.activity
import androidx.navigation.compose.composable
import com.google.android.gms.cast.framework.CastButtonFactory
import com.sf.tadami.crash.CrashLogUtil
import com.sf.tadami.crash.CrashReporter
import com.sf.tadami.navigation.graphs.settings.AdvancedSettingsRoutes
import com.sf.tadami.network.utils.setDefaultSettings
import com.sf.tadami.preferences.appearance.ThemeMode
import com.sf.tadami.preferences.appearance.setAppCompatDelegateThemeMode
import com.sf.tadami.preferences.model.Preference
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.crash_logs_dump_failed
import com.sf.tadami.shared.resources.file_picker_error
import com.sf.tadami.shared.resources.file_picker_uri_permission_unsupported
import com.sf.tadami.ui.animeinfos.episode.EpisodeActivity
import com.sf.tadami.ui.animeinfos.episode.cast.castContextOrNull
import com.sf.tadami.ui.animeinfos.episode.cast.setCastIconTint
import com.sf.tadami.ui.main.onboarding.steps.OnboardingStep
import com.sf.tadami.ui.main.onboarding.steps.PermissionStep
import com.sf.tadami.ui.tabs.more.settings.screens.advanced.adBlockGroup
import com.sf.tadami.ui.tabs.more.settings.screens.advanced.backgroundJobGroup
import com.sf.tadami.ui.tabs.more.settings.screens.advanced.background.WorkerInfosScreen
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.webview.WebViewActivity
import com.sf.tadami.utils.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The player and the browser are activities, the cast button the Google Cast one. */
class AndroidPlatformUi(private val app: Application) : PlatformUi {

    override fun NavGraphBuilder.episodeDestination(route: String, navController: NavHostController) {
        activity(route) {
            this.activityClass = EpisodeActivity::class
            argument("sourceId") {
                this.nullable = false
                this.type = NavType.LongType
            }
            argument("episode") {
                this.nullable = false
                this.type = NavType.LongType
            }
        }
    }

    override fun NavGraphBuilder.webViewDestination(route: String, navController: NavHostController) {
        activity(route) {
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
    }

    // No cast button without working Google Play services
    override val hasCastButton: Boolean by lazy { castContextOrNull(app) != null }

    @Composable
    override fun CastButton(tint: Color, modifier: Modifier) {
        val argb = tint.toArgb()
        AndroidView(
            modifier = modifier,
            factory = { context -> MediaRouteButton(context) },
            update = { mediaButton ->
                CastButtonFactory.setUpMediaRouteButton(app, mediaButton)
                mediaButton.setCastIconTint(argb)
            },
        )
    }

    @Composable
    override fun rememberFolderPicker(onPicked: (uri: String) -> Unit): () -> Unit {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri != null) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (e: SecurityException) {
                    Log.d("StorageLocationPicker", e.stackTraceToString())
                    UiToasts.showToast(Res.string.file_picker_uri_permission_unsupported)
                    // The folder would be out of reach after a restart: keep the current one
                    return@rememberLauncherForActivityResult
                }
                onPicked(uri.toString())
            }
        }
        return {
            try {
                launcher.launch(null)
            } catch (e: ActivityNotFoundException) {
                UiToasts.showToast(Res.string.file_picker_error)
            }
        }
    }

    @Composable
    override fun rememberFilePicker(title: String, onPicked: (uri: String?) -> Unit): () -> Unit {
        val launcher = rememberLauncherForActivityResult(
            object : ActivityResultContracts.GetContent() {
                override fun createIntent(context: Context, input: String): Intent {
                    val intent = super.createIntent(context, input)
                    return Intent.createChooser(intent, title)
                }
            },
        ) { onPicked(it?.toString()) }
        // no need to catch because it's wrapped with a chooser
        return { launcher.launch("*/*") }
    }

    @Composable
    override fun rememberFileSaver(mimeType: String, onPicked: (uri: String) -> Unit): (name: String) -> Unit {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument(mimeType),
        ) {
            if (it != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                } catch (e: SecurityException) {
                    // Some file pickers can't grant it: the access the picker gave still writes the file now
                    Log.d("FileSaver", e.stackTraceToString())
                }
                onPicked(it.toString())
            }
        }
        return { name ->
            try {
                launcher.launch(name)
            } catch (e: ActivityNotFoundException) {
                UiToasts.showToast(Res.string.file_picker_error)
            }
        }
    }

    // Android 13 and later confirm a copy themselves
    override val confirmsCopies: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun applyThemeMode(mode: ThemeMode) = setAppCompatDelegateThemeMode(mode)

    @Composable
    override fun rememberCloseApp(): () -> Unit {
        val activity = LocalActivity.current
        return { activity?.finish() }
    }

    override fun permissionStep(): OnboardingStep = PermissionStep()

    @Composable
    override fun rememberCrashLogsDump(): suspend () -> Unit {
        val context = LocalContext.current
        return {
            val crashLog = withContext(Dispatchers.IO) { CrashReporter.readPending(context) }
            if (crashLog == null) {
                CrashLogUtil(context).dumpLogs()
            } else {
                // The report as it was written, not this session's info
                val shared = withContext(Dispatchers.IO) {
                    CrashReporter.shareReport(context, crashLog.format())
                }
                if (shared) {
                    withContext(Dispatchers.IO) { CrashReporter.clearPending(context) }
                } else {
                    UiToasts.showToast(Res.string.crash_logs_dump_failed)
                }
            }
        }
    }

    override fun clearWebViewData() {
        WebView(app).run {
            setDefaultSettings()
            clearCache(true)
            clearFormData()
            clearHistory()
            clearSslPreferences()
            destroy()
        }
        WebStorage.getInstance().deleteAllData()
    }

    @Composable
    override fun advancedPreferenceGroups(navController: NavHostController): List<Preference> =
        listOf(adBlockGroup(), backgroundJobGroup(navController))

    override fun NavGraphBuilder.advancedDestinations(navController: NavHostController) {
        composable(
            route = AdvancedSettingsRoutes.PROCESS_INFOS,
        ) {
            WorkerInfosScreen(navController).Content()
        }
    }
}
