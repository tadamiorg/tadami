package com.sf.tadami.crash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.sf.tadami.ui.main.MainActivity
import com.sf.tadami.ui.themes.TadamiTheme
import com.sf.tadami.ui.utils.setComposeContent


class CrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val exception = GlobalExceptionHandler.getThrowableFromIntent(intent)
        setComposeContent {
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
                CrashScreen(
                    exception = exception,
                    onRestartClick = {
                        finishAffinity()
                        startActivity(Intent(this@CrashActivity, MainActivity::class.java))
                    },
                )
            }
        }
    }
}
