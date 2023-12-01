package com.sf.tadami.ui.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.sf.tadami.AppPreferences
import com.sf.tadami.Migrations
import com.sf.tadami.R
import com.sf.tadami.data.providers.DataStoreProvider
import com.sf.tadami.navigation.HomeScreen
import com.sf.tadami.notifications.cast.CastProxyService
import com.sf.tadami.ui.animeinfos.episode.cast.channels.ErrorChannel
import com.sf.tadami.ui.animeinfos.episode.cast.setCastCustomChannel
import com.sf.tadami.ui.tabs.settings.externalpreferences.source.SourcesPreferences
import com.sf.tadami.ui.tabs.settings.screens.backup.BackupPreferences
import com.sf.tadami.ui.tabs.settings.screens.library.LibraryPreferences
import com.sf.tadami.ui.tabs.settings.screens.player.PlayerPreferences
import com.sf.tadami.ui.themes.TadamiTheme
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
    private val dataStoreProvider : DataStoreProvider = Injekt.get()

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
                    dataStoreProvider = dataStoreProvider,
                    libraryPreferences = dataStoreProvider.getPreferencesGroup(LibraryPreferences),
                    playerPreferences = dataStoreProvider.getPreferencesGroup(PlayerPreferences),
                    backupPreferences = dataStoreProvider.getPreferencesGroup(BackupPreferences),
                    appPreferences = dataStoreProvider.getPreferencesGroup(AppPreferences),
                    sourcesPreferences = dataStoreProvider.getPreferencesGroup(SourcesPreferences)
                )
            }
        }

        castContext = CastContext.getSharedInstance(this)
        castSession = castContext.sessionManager.currentCastSession

        setupCastListener()

        setContent {
            TadamiTheme {
                val navController = rememberNavController()
                AppUpdaterScreen()
                HomeScreen(navController)
                LaunchedEffect(navController) {
                    if (isLaunch) {
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