package com.sf.tadami.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.luminance
import com.sf.tadami.R
import com.sf.tadami.ui.main.MainActivity
import com.sf.tadami.ui.utils.setComposeContent


class CrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prefer the persisted report — it carries the device info, extension state and recent
        // logs. The intent extra is the fallback for when writing to disk failed.
        val report = CrashReporter.readPending(this)?.format()
            ?: GlobalExceptionHandler.getThrowableFromIntent(intent)?.stackTraceToString()
            ?: getString(R.string.crash_screen_no_details)

        setComposeContent {
            val statusBarBackgroundColor = MaterialTheme.colorScheme.surface
            val isSystemInDarkTheme = isSystemInDarkTheme()

            LaunchedEffect(isSystemInDarkTheme, statusBarBackgroundColor) {
                // Draw edge-to-edge and set system bars color to transparent
                val lightStyle = SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.BLACK
                )
                val darkStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(
                    statusBarStyle = if (statusBarBackgroundColor.luminance() > 0.5) lightStyle else darkStyle,
                    navigationBarStyle = if (isSystemInDarkTheme) darkStyle else lightStyle,
                )
            }
            CrashScreen(
                report = report,
                onRestartClick = {
                    // Leave the report on disk: MainActivity offers to share it on next launch,
                    // which is the only chance to catch a crash that happened before this screen.
                    finishAffinity()
                    startActivity(Intent(this@CrashActivity, MainActivity::class.java))
                },
            )
        }
    }
}
