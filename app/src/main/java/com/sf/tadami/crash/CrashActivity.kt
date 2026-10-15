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
import com.sf.tadami.extension.util.ExtensionsGuard
import com.sf.tadami.ui.main.MainActivity
import com.sf.tadami.ui.utils.setComposeContent


class CrashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The report written for this crash, not an older one left on disk
        val timestamp = GlobalExceptionHandler.getTimestampFromIntent(intent)
        val crashLog = CrashReporter.readPending(this)?.takeIf { it.timestamp == timestamp }
        // Shown here, no need to offer it again on the next launch
        crashLog?.let { CrashReporter.markOffered(this, it) }
        val report = crashLog?.format()
            ?: GlobalExceptionHandler.getThrowableFromIntent(intent)?.message
            ?: getString(R.string.crash_screen_no_details)
        // The culprits are shown above it
        val shownReport = crashLog?.format(withCulprits = false) ?: report
        val culprits = crashLog?.culprits ?: GlobalExceptionHandler.getCulpritsFromIntent(intent)
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
                report = shownReport,
                sharedReport = report,
                culprits = culprits,
                onReportShared = {
                    if (crashLog != null) CrashReporter.clearPending(this@CrashActivity)
                },
                onRestartClick = {
                    CrashReporter.clearPending(this@CrashActivity)
                    restart()
                },
                onRestartWithoutExtensionsClick = {
                    // Read by the next launch, nothing of the extensions is built in this process
                    runCatching { ExtensionsGuard.setSafeMode(this@CrashActivity, true) }
                    CrashReporter.clearPending(this@CrashActivity)
                    restart()
                },
            )
        }
    }

    /** Starts the app in its own task, this one being removed from the recent apps. */
    private fun restart() {
        finishAndRemoveTask()
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
