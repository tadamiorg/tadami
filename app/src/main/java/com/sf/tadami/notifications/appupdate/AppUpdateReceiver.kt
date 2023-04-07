package com.sf.tadami.notifications.appupdate

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sf.tadami.utils.notificationManager

class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when(intent?.action){
            ACTION_DISMISS_NOTIFICATION -> context.notificationManager.cancel(intent.getIntExtra(EXTRA_NOTIFICATION_ID,-1))
        }
        AppUpdateWorker.stop(context)
    }

    companion object{
        private const val BROADCAST_REQUEST_CODE = 0
        private const val ACTION_DISMISS_NOTIFICATION = "action_dismiss_notification"
        private const val EXTRA_NOTIFICATION_ID = "tadami_extra_action_dismiss_notification_id"
        fun getPendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                BROADCAST_REQUEST_CODE,
                Intent(context, AppUpdateReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        fun dismissNotification(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, AppUpdateReceiver::class.java).apply {
                action = ACTION_DISMISS_NOTIFICATION
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}