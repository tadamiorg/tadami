package com.sf.tadami.crash

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.sf.tadami.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Offers the previous session's crash report on the next normal launch.
 *
 * The crash screen itself is easy to miss — the process is dying, and users usually just relaunch —
 * so without this most reports would never leave the device.
 */
@Composable
fun PendingCrashDialog() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val hasPending by produceState(initialValue = false) {
        value = withContext(Dispatchers.IO) { CrashReporter.hasPending(context) }
    }
    var dismissed by remember { mutableStateOf(false) }

    if (!hasPending || dismissed) return

    AlertDialog(
        onDismissRequest = { dismissed = true },
        title = { Text(text = stringResource(R.string.crash_pending_title, stringResource(R.string.app_name))) },
        text = { Text(text = stringResource(R.string.crash_pending_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    dismissed = true
                    scope.launch {
                        CrashLogUtil(context).dumpLogs()
                        CrashReporter.clearPending(context)
                    }
                },
            ) {
                Text(text = stringResource(R.string.crash_pending_share))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    dismissed = true
                    scope.launch {
                        withContext(Dispatchers.IO) { CrashReporter.clearPending(context) }
                    }
                },
            ) {
                Text(text = stringResource(R.string.crash_pending_dismiss))
            }
        },
    )
}
