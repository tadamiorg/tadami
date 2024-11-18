package com.sf.tadami.notifications.appupdate

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sf.tadami.utils.cancelNotification
import com.sf.tadami.utils.notificationManager

class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when(intent?.action){
            ACTION_DISMISS_NOTIFICATION -> dismissNotification(context, intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1))
            ACTION_CANCEL_APP_UPDATE_DOWNLOAD -> cancelDownloadAppUpdate(context)
            ACTION_START_APP_UPDATE -> {
                val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
                dismissNotification(context,notifId)
                startDownloadAppUpdate(context, intent)
            }
        }
    }

    private fun cancelDownloadAppUpdate(context: Context) {
        AppUpdateWorker.stop(context)
    }

    private fun startDownloadAppUpdate(context: Context, intent: Intent) {
        val url = intent.getStringExtra(AppUpdateWorker.EXTRA_DOWNLOAD_URL) ?: return
        AppUpdateWorker.start(context, url)
    }

    companion object{
        private const val BROADCAST_REQUEST_CODE = 0
        private const val ACTION_DISMISS_NOTIFICATION = "action_dismiss_notification"
        private const val EXTRA_NOTIFICATION_ID = "tadami_extra_action_dismiss_notification_id"
        private const val ACTION_CANCEL_APP_UPDATE_DOWNLOAD = "appUpdate.CANCEL_APP_UPDATE_DOWNLOAD"
        private const val ACTION_START_APP_UPDATE = "appUpdate.ACTION_START_APP_UPDATE"

        fun downloadAppUpdatePendingBroadcast(
            context: Context,
            url: String,
            title: String? = null,
            notificationId: Int
        ): PendingIntent {
            return Intent(context, AppUpdateReceiver::class.java).run {
                action = ACTION_START_APP_UPDATE
                putExtra(AppUpdateWorker.EXTRA_DOWNLOAD_URL, url)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                title?.let { putExtra(AppUpdateWorker.EXTRA_DOWNLOAD_TITLE, it) }
                PendingIntent.getBroadcast(
                    context,
                    0,
                    this,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
        }

        fun cancelDownloadAppUpdatePendingBroadcast(context: Context): PendingIntent {
            val intent = Intent(context, AppUpdateReceiver::class.java).apply {
                action = ACTION_CANCEL_APP_UPDATE_DOWNLOAD
            }
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        fun dismissNotificationPendingBroadcast(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, AppUpdateReceiver::class.java).apply {
                action = ACTION_DISMISS_NOTIFICATION
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(
                context,
                BROADCAST_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }


        fun dismissNotification(context: Context, notificationId: Int, groupId: Int? = null) {
            val groupKey = context.notificationManager.activeNotifications.find {
                it.id == notificationId
            }?.groupKey

            if (groupId != null && groupId != 0 && !groupKey.isNullOrEmpty()) {
                val notifications = context.notificationManager.activeNotifications.filter {
                    it.groupKey == groupKey
                }
                if (notifications.size == 2) {
                    context.cancelNotification(groupId)
                    return
                }
            }
            context.cancelNotification(notificationId)
        }
    }
}