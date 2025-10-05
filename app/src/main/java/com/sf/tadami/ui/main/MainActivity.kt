package com.sf.tadami.ui.main

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.sf.tadami.AppPreferences
import com.sf.tadami.Migrations
import com.sf.tadami.R
import com.sf.tadami.extension.api.ExtensionsApi
import com.sf.tadami.navigation.HomeScreen
import com.sf.tadami.navigation.graphs.onboarding.OnboardingRoutes
import com.sf.tadami.notifications.cast.CastProxyService
import com.sf.tadami.preferences.app.BasePreferences
import com.sf.tadami.preferences.appearance.AppearancePreferences
import com.sf.tadami.preferences.backup.BackupPreferences
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.ui.animeinfos.episode.cast.channels.ErrorChannel
import com.sf.tadami.ui.animeinfos.episode.cast.setCastCustomChannel
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.utils.setComposeContent
import com.sf.tadami.utils.editPreference
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MainActivity : AppCompatActivity() {

    private var castSession: CastSession? = null
    private var castSessionManagerListener: SessionManagerListener<CastSession>? = null
    private lateinit var castContext: CastContext
    private val errorChannel = ErrorChannel()
    private var ready = false
    private val dataStore: DataStore<Preferences> = Injekt.get()
    private val sourcesManager: SourceManager = Injekt.get()

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        val isLaunch = savedInstanceState == null
        val splashScreen = if (isLaunch) installSplashScreen() else null

        val prefTheme =
            runBlocking { dataStore.getPreferencesGroup(AppearancePreferences).appTheme }

        val theme = when (prefTheme.name) {
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
        setTheme(theme)

        super.onCreate(savedInstanceState)

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

        castContext = CastContext.getSharedInstance(this)
        castSession = castContext.sessionManager.currentCastSession

        setupCastListener()

        setComposeContent {
            val statusBarBackgroundColor = MaterialTheme.colorScheme.surface
            val isSystemInDarkTheme = isSystemInDarkTheme()

            LaunchedEffect(isSystemInDarkTheme, statusBarBackgroundColor) {
                // Draw edge-to-edge and set system bars color to transparent
                val lightStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
                val darkStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                enableEdgeToEdge(
                    statusBarStyle = if (statusBarBackgroundColor.luminance() > 0.5) lightStyle else darkStyle,
                    navigationBarStyle = if (isSystemInDarkTheme) darkStyle else lightStyle,
                )
            }
            val navController = rememberNavController()

            val basePreferencesState = rememberDataStoreState(BasePreferences)
            val basePreferences by basePreferencesState.value.collectAsState()

            AppUpdaterScreen()
            ExtensionsCheckForUpdates()
            HomeScreen(navController)
            LaunchedEffect(navController) {
                if (isLaunch) {
                    if (!basePreferences.onboardingComplete) {
                        navController.navigate(OnboardingRoutes.ONBOARDING)
                    }
                    ready = true
                }
            }
        }

        val startTime = System.currentTimeMillis()
        splashScreen?.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - startTime
            elapsed <= SPLASH_MIN_DURATION || (!ready && elapsed <= SPLASH_MAX_DURATION)
        }
    }

    @Composable
    private fun ExtensionsCheckForUpdates() {
        val context = LocalContext.current

        // Extensions updates
        LaunchedEffect(Unit) {
            try {
                val extsUpdates = ExtensionsApi().checkForUpdates(context)
                dataStore.editPreference(
                    extsUpdates?.size ?: 0,
                    SourcesPreferences.EXT_UPDATES_COUNT
                )
            } catch (e: Exception) {
                Log.e("ExtensionsCheckForUpdates", e.stackTraceToString())
            }
        }
    }

    override fun onResume() {
        castContext.sessionManager.addSessionManagerListener(
            castSessionManagerListener!!,
            CastSession::class.java
        )
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        castContext.sessionManager.removeSessionManagerListener(
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
                onApplicationDisconnected()
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                onApplicationConnected(session)
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionSuspended(session: CastSession, reason: Int) {}

            @OptIn(UnstableApi::class)
            private fun onApplicationConnected(session: CastSession) {
                setCastCustomChannel(session, errorChannel)
                CastProxyService.startNow(this@MainActivity)
                this@MainActivity.castSession = session
            }

            @OptIn(UnstableApi::class)
            private fun onApplicationDisconnected() {
                CastProxyService.stop(this@MainActivity)
                this@MainActivity.castSession = null
            }
        }
    }

    companion object {
        const val SPLASH_MIN_DURATION = 500 // ms
        const val SPLASH_MAX_DURATION = 5000 // ms
    }
}