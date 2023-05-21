package com.sf.tadami.ui.main

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.sf.tadami.R
import com.sf.tadami.navigation.HomeScreen
import com.sf.tadami.notifications.cast.CastProxyService
import com.sf.tadami.ui.themes.TadamiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var navLoaded: Boolean = true
    private var castSession: CastSession? = null
    private var castSessionManagerListener: SessionManagerListener<CastSession>? = null
    private lateinit var castContext: CastContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        castContext = CastContext.getSharedInstance(this)
        castSession = castContext.sessionManager.currentCastSession

        setupCastListener()

        if (savedInstanceState != null) {
            with(savedInstanceState) {
                navLoaded = getBoolean(STATE_NAV_LOADED)
            }
            setTheme(R.style.Theme_Tadami)
        } else {
            installSplashScreen().apply {
                setKeepOnScreenCondition {
                    navLoaded
                }
            }
        }

        setContent {
            val coroutineScope = rememberCoroutineScope()

            TadamiTheme {
                AppUpdaterScreen()
                HomeScreen(navController = rememberNavController(), navLoaded = {
                    if (this.navLoaded) {
                        coroutineScope.launch(Dispatchers.IO) {
                            delay(250)
                            this@MainActivity.navLoaded = false
                        }
                    }
                })
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.run {
            putBoolean(STATE_NAV_LOADED, navLoaded)
        }
        super.onSaveInstanceState(outState, outPersistentState)
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
                onApplicationDisconnected(session)
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                onApplicationConnected(session)
            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                onApplicationDisconnected(session)
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                onApplicationConnected(session)
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                onApplicationDisconnected(session)
            }

            override fun onSessionStarting(session: CastSession) {}
            override fun onSessionEnding(session: CastSession) {}
            override fun onSessionResuming(session: CastSession, sessionId: String) {}
            override fun onSessionSuspended(session: CastSession, reason: Int) {}
            private fun onApplicationConnected(castSession: CastSession) {
                CastProxyService.startNow(this@MainActivity)
                this@MainActivity.castSession = castSession
            }

            private fun onApplicationDisconnected(session: CastSession) {
                CastProxyService.stop(this@MainActivity)
                this@MainActivity.castSession = null
            }
        }
    }

    companion object {
        const val STATE_NAV_LOADED = "nav_loaded"
    }
}