package com.sf.tadami.crash

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
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
import com.sf.tadami.ui.components.modal.ModalAction
import com.sf.tadami.ui.components.modal.ModalBodyText
import com.sf.tadami.ui.components.modal.TadaDialog
import com.sf.tadami.ui.utils.UiToasts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Offers the crash report of a previous session the crash screen never showed, e.g. because it
 * failed to start.
 */
@Composable
fun PendingCrashDialog() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val crashLog by produceState<CrashReporter.CrashLog?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            CrashReporter.readPending(context)?.takeIf { !it.offered }
        }
    }
    var dismissed by remember { mutableStateOf(false) }

    val log = crashLog
    if (log == null || dismissed) return

    val onDismiss = {
        dismissed = true
        // Still shareable from the advanced settings
        scope.launch(Dispatchers.IO) { CrashReporter.markOffered(context, log) }
        Unit
    }

    TadaDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.crash_pending_title, stringResource(R.string.app_name)),
        icon = Icons.Outlined.BugReport,
        confirm = ModalAction(stringResource(R.string.crash_pending_share)) {
            dismissed = true
            scope.launch {
                // The report as it was written, not this session's info
                val shared = withContext(Dispatchers.IO) {
                    CrashReporter.shareReport(context, log.format())
                }
                if (shared) {
                    withContext(Dispatchers.IO) { CrashReporter.clearPending(context) }
                } else {
                    UiToasts.showToast(R.string.crash_logs_dump_failed)
                }
            }
        },
        dismiss = ModalAction(stringResource(R.string.crash_pending_dismiss), onClick = onDismiss),
    ) {
        ModalBodyText(stringResource(R.string.crash_pending_message))
    }
}
