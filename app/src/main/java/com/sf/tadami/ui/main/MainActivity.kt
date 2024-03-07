package com.sf.tadami.ui.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.sf.tadami.AppPreferences
import com.sf.tadami.Migrations
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.app.AppScreen
import com.sf.tadami.preferences.backup.BackupPreferences
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.preferences.sources.SourcesPreferences
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.themes.TadamiTheme
import com.sf.tadami.utils.getPreferencesGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MainActivity : AppCompatActivity() {

    private var ready = false
    private val dataStore : DataStore<Preferences> = Injekt.get()
    private val sourcesManager : SourceManager = Injekt.get()

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

        setContent {

            TadamiTheme {
                val navController = rememberNavController()
                AppUpdaterScreen()
                AppScreen(navController)
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

    companion object {
        const val SPLASH_MIN_DURATION = 500 // ms
        const val SPLASH_MAX_DURATION = 5000 // ms
    }
}