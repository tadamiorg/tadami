package com.sf.tadami.notifications.backup

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sf.tadami.ui.utils.toShareIntent

class BackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_CANCEL_RESTORE -> cancelRestore(context)
        }
    }

    private fun cancelRestore(context: Context) {
        BackupRestoreWorker.stop(context)
    }


    companion object {
        private const val BROADCAST_REQUEST_CODE = 0
        private const val ACTION_CANCEL_RESTORE = "action_cancel_restore"
        private const val EXTRA_NOTIFICATION_ID = "tadami_action_extra__notification_id"

        // Opens the share sheet itself: since Android 12 a receiver can't start it from a notification action
        fun shareBackupPendingActivity(context: Context, uri: Uri): PendingIntent {
            return PendingIntent.getActivity(
                context,
                0,
                uri.toShareIntent(context, "application/x-protobuf+gzip"),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        fun cancelRestorePendingBroadcast(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, BackupReceiver::class.java).apply {
                action = ACTION_CANCEL_RESTORE
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(
                context,
                BROADCAST_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        internal fun openErrorLogPendingActivity(context: Context, uri: Uri): PendingIntent {
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, "text/plain")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }
}