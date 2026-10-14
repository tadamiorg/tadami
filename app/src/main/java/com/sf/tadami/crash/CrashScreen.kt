package com.sf.tadami.crash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import com.sf.tadami.ui.main.onboarding.InfoScreen
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.ui.utils.padding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CrashScreen(
    report: String,
    onRestartClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    InfoScreen(
        icon = Icons.Outlined.BugReport,
        headingText = stringResource(R.string.crash_screen_title),
        subtitleText = stringResource(R.string.crash_screen_description, stringResource(R.string.app_name)),
        acceptText = stringResource(R.string.pref_dump_crash_logs),
        onAcceptClick = {
            scope.launch {
                // Not CrashLogUtil: building the extension manager in this process would load the
                // extensions again, maybe the one that crashed
                if (!withContext(Dispatchers.IO) { CrashReporter.shareReport(context, report) }) {
                    UiToasts.showToast(R.string.crash_logs_dump_failed)
                }
            }
        },
        rejectText = stringResource(R.string.crash_screen_restart_application),
        onRejectClick = onRestartClick,
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = MaterialTheme.padding.small)
                .clip(MaterialTheme.shapes.small)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                text = report,
                modifier = Modifier
                    .padding(all = MaterialTheme.padding.small),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
