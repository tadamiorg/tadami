package com.sf.tadami.notifications.backup

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sf.tadami.ui.utils.getParcelableExtraCompat
import com.sf.tadami.ui.utils.toShareIntent
import com.sf.tadami.utils.cancelNotification

class BackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_SHARE_BACKUP -> shareBackup(
                context,
                intent.getParcelableExtraCompat(EXTRA_URI)!!,
                "application/x-protobuf+gzip",
                intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            )
            ACTION_CANCEL_RESTORE -> cancelRestore(context)
        }
    }

    private fun cancelRestore(context: Context) {
        BackupRestoreWorker.stop(context)
    }

    private fun shareBackup(context: Context, uri: Uri, fileMimeType: String, notificationId: Int) {
        dismissNotification(context, notificationId)
        context.startActivity(uri.toShareIntent(context, fileMimeType))
    }

    private fun dismissNotification(context: Context, notificationId: Int) {
        context.cancelNotification(notificationId)
    }


    companion object {
        private const val BROADCAST_REQUEST_CODE = 0
        private const val ACTION_SHARE_BACKUP = "action_share_backup"
        private const val ACTION_CANCEL_RESTORE = "action_cancel_restore"
        private const val EXTRA_URI = "action_extra_uri"
        private const val EXTRA_NOTIFICATION_ID = "tadami_action_extra__notification_id"

        fun shareBackupPendingBroadcast(
            context: Context,
            uri: Uri,
            notificationId: Int
        ): PendingIntent {
            val intent = Intent(context, BackupReceiver::class.java).apply {
                action = ACTION_SHARE_BACKUP
                putExtra(EXTRA_URI, uri)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(
                context,
                BROADCAST_REQUEST_CODE,
                intent,
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