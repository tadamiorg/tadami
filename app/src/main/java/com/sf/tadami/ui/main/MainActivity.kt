package com.sf.tadami.ui.main

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
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
import com.sf.tadami.preferences.backup.BackupPreferences
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.model.rememberDataStoreState
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.ui.animeinfos.episode.cast.channels.ErrorChannel
import com.sf.tadami.ui.animeinfos.episode.cast.setCastCustomChannel
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.themes.TadamiTheme
import com.sf.tadami.utils.editPreference
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MainActivity : AppCompatActivity() {

    private var castSession: CastSession? = null
    private var castSessionManagerListener: SessionManagerListener<CastSession>? = null
    private lateinit var castContext: CastContext
    private val errorChannel = ErrorChannel()
    private var ready = false
    private val dataStore : DataStore<Preferences> = Injekt.get()
    private val sourcesManager : SourceManager = Injekt.get()

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val isLaunch = savedInstanceState == null
        val splashScreen = if (isLaunch) installSplashScreen() else null
        if(!isLaunch){
            setTheme(R.style.Theme_Tadami)
        }

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

        setContent {

            TadamiTheme {
                val systemUiController = rememberSystemUiController()
                val statusBarBackgroundColor = MaterialTheme.colorScheme.surface
                val navbarScrimColor = MaterialTheme.colorScheme.surfaceContainer
                val isSystemInDarkTheme = isSystemInDarkTheme()

                LaunchedEffect(systemUiController, statusBarBackgroundColor) {
                    systemUiController.setStatusBarColor(
                        color = statusBarBackgroundColor,
                        darkIcons = statusBarBackgroundColor.luminance() > 0.5,
                        transformColorForLightContent = { Color.Black },
                    )
                }
                LaunchedEffect(systemUiController, isSystemInDarkTheme, navbarScrimColor) {
                    systemUiController.setNavigationBarColor(
                        color = navbarScrimColor,
                        darkIcons = !isSystemInDarkTheme,
                        navigationBarContrastEnforced = false,
                        transformColorForLightContent = { Color.Black },
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
                dataStore.editPreference(extsUpdates?.size ?: 0,SourcesPreferences.EXT_UPDATES_COUNT)
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
            private fun onApplicationConnected(session: CastSession) {
                setCastCustomChannel(session,errorChannel)
                CastProxyService.startNow(this@MainActivity)
                this@MainActivity.castSession = session
            }

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