package com.sf.tadami.ui.main

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.window.SplashScreenView
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.children
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.mediarouter.app.MediaRouteChooserDialogFragment
import androidx.mediarouter.app.MediaRouteControllerDialogFragment
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.sf.tadami.AppPreferences
import com.sf.tadami.Migrations
import com.sf.tadami.R
import com.sf.tadami.crash.PendingCrashDialog
import com.sf.tadami.extension.api.ExtensionsApi
import com.sf.tadami.extension.util.ExtensionsGuard
import com.sf.tadami.navigation.HomeScreen
import com.sf.tadami.navigation.graphs.home.HomeNavItems
import com.sf.tadami.navigation.graphs.more.MORE_ROUTES
import com.sf.tadami.navigation.graphs.onboarding.OnboardingRoutes
import com.sf.tadami.navigation.graphs.settings.DataSettingsRoutes
import com.sf.tadami.notifications.cast.CastControlService
import com.sf.tadami.preferences.app.BasePreferences
import com.sf.tadami.preferences.appearance.AppearancePreferences
import com.sf.tadami.preferences.backup.BackupPreferences
import com.sf.tadami.preferences.cast.CastPreferences
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.ui.animeinfos.episode.cast.CastConnectionErrorDialog
import com.sf.tadami.ui.animeinfos.episode.cast.CastConnectionState
import com.sf.tadami.ui.animeinfos.episode.cast.CastSessionState
import com.sf.tadami.ui.animeinfos.episode.cast.CastWebReceiverNoticeDialog
import com.sf.tadami.ui.animeinfos.episode.cast.castContextOrNull
import com.sf.tadami.ui.animeinfos.episode.cast.channels.ErrorChannel
import com.sf.tadami.ui.animeinfos.episode.cast.logCastConnectionError
import com.sf.tadami.ui.animeinfos.episode.cast.setCastCustomChannel
import com.sf.tadami.ui.components.banners.DownloadedOnlyBannerBackgroundColor
import com.sf.tadami.ui.tabs.browse.BROWSE_TAB_KEY
import com.sf.tadami.ui.tabs.browse.EXTENSIONS_TAB
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.themes.AppTheme
import com.sf.tadami.ui.utils.setComposeContent
import com.sf.tadami.utils.editPreference
import com.sf.tadami.utils.editPreferences
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.getPreferencesGroupAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MainActivity : AppCompatActivity() {

    private var castSession: CastSession? = null
    private var castSessionManagerListener: SessionManagerListener<CastSession>? = null
    // Null without working Google Play services: cast is unavailable
    private var castContext: CastContext? = null
    private val errorChannel = ErrorChannel()
    private var ready = false
    private val dataStore: DataStore<Preferences> = Injekt.get()
    // Only built when used, so creating the activity doesn't wait for the extensions
    private val sourcesManager: SourceManager by lazy { Injekt.get() }
    private val openExtensionsRequests = Channel<Unit>(Channel.CONFLATED)
    private val openBackupSettingsRequests = Channel<Unit>(Channel.CONFLATED)
    private val openDownloadsRequests = Channel<Unit>(Channel.CONFLATED)
    private var nightMode = Configuration.UI_MODE_NIGHT_UNDEFINED

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        val isLaunch = savedInstanceState == null
        val splashScreen = if (isLaunch) installSplashScreen() else null

        val prefTheme =
            runBlocking { dataStore.getPreferencesGroup(AppearancePreferences).appTheme }

        setTheme(themeStyle(prefTheme))

        super.onCreate(savedInstanceState)

        // The views (the cast dialogs) take the colours of a scheme chosen later, without recreating the
        // activity. setTheme also keeps the style AppCompat applies again on a light/dark change.
        lifecycleScope.launch {
            dataStore.getPreferencesGroupAsFlow(AppearancePreferences)
                .map { themeStyle(it.appTheme) }
                .distinctUntilChanged()
                .collect { setTheme(it) }
        }
        nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        if (isLaunch) {
            lifecycleScope.launch(Dispatchers.IO) {
                Migrations.upgrade(
                    context = applicationContext,
                    dataStore = dataStore,
                    sourcesManager = sourcesManager,
                    libraryPreferences = dataStore.getPreferencesGroup(LibraryPreferences),
                    playerPreferences = dataStore.getPreferencesGroup(PlayerPreferences),
                    backupPreferences = dataStore.getPreferencesGroup(BackupPreferences),
                    appPreferences = dataStore.getPreferencesGroup(AppPreferences),
                    sourcesPreferences = dataStore.getPreferencesGroup(SourcesPreferences)
                )
            }
        }

        if (isLaunch && intent?.action == ACTION_OPEN_EXTENSIONS) {
            openExtensionsRequests.trySend(Unit)
        }
        if (isLaunch && intent?.action == ACTION_OPEN_BACKUP_SETTINGS) {
            openBackupSettingsRequests.trySend(Unit)
        }
        if (isLaunch && intent?.action == ACTION_OPEN_DOWNLOADS) {
            openDownloadsRequests.trySend(Unit)
        }

        castContext = castContextOrNull(this)
        castSession = castContext?.sessionManager?.currentCastSession

        setupCastListener()

        setComposeContent {
            val basePreferencesState = rememberDataStoreState(BasePreferences)
            val basePreferences by basePreferencesState.value.collectAsState()

            // The downloaded only banner is drawn behind the status bar
            val statusBarBackgroundColor = if (basePreferences.downloadedOnly) {
                DownloadedOnlyBannerBackgroundColor
            } else {
                MaterialTheme.colorScheme.surface
            }
            val isSystemInDarkTheme = isSystemInDarkTheme()

            // Before the frame is drawn: the bar icons change along with the theme
            DisposableEffect(isSystemInDarkTheme, statusBarBackgroundColor) {
                // Draw edge-to-edge and set system bars color to transparent
                val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
                val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                enableEdgeToEdge(
                    statusBarStyle = if (statusBarBackgroundColor.luminance() > 0.5) lightStyle else darkStyle,
                    navigationBarStyle = if (isSystemInDarkTheme) darkStyle else lightStyle,
                )
                onDispose {}
            }
            val navController = rememberNavController()

            AppUpdaterScreen(navController)
            ExtensionsCheckForUpdates()
            PendingCrashDialog()
            val castConnectionError by CastConnectionState.error.collectAsState()
            if (castConnectionError) {
                CastConnectionErrorDialog(
                    onDismissRequest = { CastConnectionState.clear() }
                )
            }
            val showWebReceiverNotice by CastSessionState.showWebReceiverNotice.collectAsState()
            if (showWebReceiverNotice) {
                CastWebReceiverNoticeDialog(
                    onDismissRequest = { dontShowAgain ->
                        CastSessionState.showWebReceiverNotice.value = false
                        if (dontShowAgain) {
                            lifecycleScope.launch {
                                dataStore.editPreferences(
                                    CastPreferences(showWebReceiverNotice = false),
                                    CastPreferences,
                                )
                            }
                        }
                    }
                )
            }
            HomeScreen(navController, downloadedOnly = basePreferences.downloadedOnly)
            LaunchedEffect(navController) {
                if (isLaunch) {
                    if (!basePreferences.onboardingComplete) {
                        navController.navigate(OnboardingRoutes.ONBOARDING)
                    }
                    ready = true
                }
            }
            LaunchedEffect(navController) {
                for (request in openExtensionsRequests) {
                    if (!basePreferences.onboardingComplete) continue
                    navController.navigate(HomeNavItems.Browse.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    navController.getBackStackEntry(HomeNavItems.Browse.route)
                        .savedStateHandle[BROWSE_TAB_KEY] = EXTENSIONS_TAB
                }
            }
            LaunchedEffect(navController) {
                for (request in openBackupSettingsRequests) {
                    if (!basePreferences.onboardingComplete) continue
                    navController.navigate(DataSettingsRoutes.HOME) {
                        launchSingleTop = true
                    }
                }
            }
            LaunchedEffect(navController) {
                for (request in openDownloadsRequests) {
                    if (!basePreferences.onboardingComplete) continue
                    navController.navigate(MORE_ROUTES.DOWNLOAD_QUEUE) {
                        launchSingleTop = true
                    }
                }
            }
        }

        val startTime = System.currentTimeMillis()
        splashScreen?.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - startTime
            elapsed <= SPLASH_MIN_DURATION || (!ready && elapsed <= SPLASH_MAX_DURATION)
        }
        splashScreen?.setOnExitAnimationListener { provider ->
            // The logo first ends its entrance (Android 12+), then grows a little as the splash fades out.
            // The view animations follow the animator duration scale: they end at once when it's 0.
            // The start is a wall clock time (0 below Android 12, where the icon doesn't move). The icon follows
            // the animator duration scale too, its reported duration doesn't.
            val iconDuration = (provider.iconAnimationDurationMillis * ValueAnimator.getDurationScale()).toLong()
            val delay = (provider.iconAnimationStartMillis + iconDuration - System.currentTimeMillis()).coerceIn(0L, iconDuration)
            // The logo fades on its own, a bit faster than the splash, so it is never left on a faded background:
            // on Android 12+ it is drawn by a surface that the alpha of the splash view doesn't reach.
            // Some Android 12 launches show no icon: the view is then missing
            runCatching { provider.iconView }.getOrNull()?.animate()
                ?.alpha(0f)
                ?.scaleX(SPLASH_EXIT_ICON_SCALE)
                ?.scaleY(SPLASH_EXIT_ICON_SCALE)
                ?.setStartDelay(delay)
                ?.setDuration(SPLASH_EXIT_ICON_DURATION)
                ?.start()
            provider.view.animate()
                .alpha(0f)
                .setStartDelay(delay)
                .setDuration(SPLASH_EXIT_DURATION)
                .withEndAction { provider.remove() }
                .start()
        }
        if (splashScreen != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            holdFramesUntilSplashCopy()
        }
    }

    /**
     * The first frame drawn once the splash can leave makes the system hand it over: a copy of the splash is
     * attached to the window, and the system splash is hidden along with the frame that draws it. Some devices
     * (OnePlus) hide it along with the next frame sent instead, so a frame drawn before the copy shows the screen
     * under the splash for a frame. Until the copy is attached, the window draws no other frame. The wait ends
     * anyway if the copy doesn't come.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun holdFramesUntilSplashCopy() {
        val decorView = window.decorView as ViewGroup
        var firstFrameTime = 0L
        val onDrawListener = ViewTreeObserver.OnDrawListener {
            if (firstFrameTime == 0L) firstFrameTime = System.currentTimeMillis()
        }
        decorView.viewTreeObserver.addOnDrawListener(onDrawListener)
        decorView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (firstFrameTime == 0L) return true
                if (decorView.children.none { it is SplashScreenView } &&
                    System.currentTimeMillis() - firstFrameTime <= SPLASH_COPY_TIMEOUT
                ) {
                    return false
                }
                decorView.viewTreeObserver.removeOnDrawListener(onDrawListener)
                decorView.viewTreeObserver.removeOnPreDrawListener(this)
                return true
            }
        })
    }

    private fun themeStyle(appTheme: AppTheme): Int = when (appTheme.name) {
        "DEFAULT" -> R.style.Theme_Tadami
        "GREEN_APPLE" -> R.style.Theme_Tadami_GreenApple
        "LAVENDER" -> R.style.Theme_Tadami_Lavender
        "MIDNIGHT_DUSK" -> R.style.Theme_Tadami_MidnightDusk
        "STRAWBERRY_DAIQUIRI" -> R.style.Theme_Tadami_Strawberry
        "TAKO" -> R.style.Theme_Tadami_Tako
        "TEALTURQUOISE" -> R.style.Theme_Tadami_TealTurquoise
        "TIDAL_WAVE" -> R.style.Theme_Tadami_TidalWave
        "YINYANG" -> R.style.Theme_Tadami_YinYang
        "YOTSUBA" -> R.style.Theme_Tadami_Yotsuba
        "DOOM" -> R.style.Theme_Tadami_Doom
        else -> R.style.Theme_Tadami
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // AppCompat has applied the light/dark mode to the resources and the theme by now
        val newNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (newNightMode != nightMode) {
            nightMode = newNightMode
            // An open cast dialog keeps the look it was created with: close it, the next one is right
            supportFragmentManager.fragments
                .filter { it is MediaRouteChooserDialogFragment || it is MediaRouteControllerDialogFragment }
                .forEach { (it as DialogFragment).dismissAllowingStateLoss() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_OPEN_EXTENSIONS) {
            openExtensionsRequests.trySend(Unit)
        }
        if (intent.action == ACTION_OPEN_BACKUP_SETTINGS) {
            openBackupSettingsRequests.trySend(Unit)
        }
        if (intent.action == ACTION_OPEN_DOWNLOADS) {
            openDownloadsRequests.trySend(Unit)
        }
    }

    @Composable
    private fun ExtensionsCheckForUpdates() {
        val context = LocalContext.current

        // Extensions updates
        LaunchedEffect(Unit) {
            try {
                val extsUpdates = ExtensionsApi().checkForUpdates(context)
                // null when the check was skipped (throttled), keep the current count then
                if (extsUpdates != null) {
                    dataStore.editPreference(
                        extsUpdates.size,
                        SourcesPreferences.EXT_UPDATES_COUNT
                    )
                }
            } catch (e: Exception) {
                Log.e("ExtensionsCheckForUpdates", e.stackTraceToString())
            }
        }
    }

    override fun onResume() {
        ExtensionsGuard.onActivityResumed(applicationContext)
        castContext?.sessionManager?.addSessionManagerListener(
            castSessionManagerListener!!,
            CastSession::class.java
        )
        // addSessionManagerListener doesn't replay for an already-live session, so cover the case of
        // resuming onto an existing cast session.
        if (castContext?.sessionManager?.currentCastSession?.isConnected == true) {
            CastControlService.startNow(this)
        }
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        castContext?.sessionManager?.removeSessionManagerListener(
            castSessionManagerListener!!,
            CastSession::class.java
        )
        castSession = null
    }

    private fun setupCastListener() {
        castSessionManagerListener = object : SessionManagerListener<CastSession> {
            override fun onSessionEnded(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                onApplicationConnected(session)
            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                logCastConnectionError("Session resume", error)
                CastConnectionState.notifyError()
                onApplicationDisconnected()
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                onApplicationConnected(session)
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                logCastConnectionError("Session start", error)
                CastConnectionState.notifyError()
                onApplicationDisconnected()
            }

            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionSuspended(session: CastSession, reason: Int) {}

            @OptIn(UnstableApi::class)
            private fun onApplicationConnected(session: CastSession) {
                setCastCustomChannel(session, errorChannel)
                this@MainActivity.castSession = session
                // Start the control service as soon as any session connects (from any screen), not
                // only when an episode is loaded. It owns TV episode switching and self-stops on end.
                CastControlService.startNow(this@MainActivity)
            }

            @OptIn(UnstableApi::class)
            private fun onApplicationDisconnected() {
                this@MainActivity.castSession = null
            }
        }
    }

    companion object {
        const val SPLASH_MIN_DURATION = 500 // ms
        const val SPLASH_MAX_DURATION = 5000 // ms
        const val SPLASH_EXIT_DURATION = 300L // ms
        const val SPLASH_EXIT_ICON_DURATION = 200L // ms
        const val SPLASH_EXIT_ICON_SCALE = 1.2f
        const val SPLASH_COPY_TIMEOUT = 500L // ms

        /** Opens the extensions tab, from the extension updates notification. */
        const val ACTION_OPEN_EXTENSIONS = "com.sf.tadami.EXTENSIONS"

        /** Opens the backup settings, from the automatic backup error notification. */
        const val ACTION_OPEN_BACKUP_SETTINGS = "com.sf.tadami.BACKUP_SETTINGS"

        /** Opens the download queue, from the download notifications. */
        const val ACTION_OPEN_DOWNLOADS = "com.sf.tadami.DOWNLOADS"
    }
}