package com.sf.tadami.ui.main

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.sf.tadami.R
import com.sf.tadami.navigation.HomeScreen
import com.sf.tadami.ui.themes.TadamiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var navLoaded: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    companion object {
        const val STATE_NAV_LOADED = "nav_loaded"
    }
}