package com.sf.tadami.notifications.cast

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.cast.framework.CastContext
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.utils.notificationManager

class CastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when(intent?.action){
            ACTION_DISMISS_NOTIFICATION -> {
                val castContext = CastContext.getSharedInstance(context)
                context.notificationManager.cancel(Notifications.CAST_PROXY_STATUS_ID)
                castContext.sessionManager.endCurrentSession(true)
            }
        }
    }

    companion object{
        private const val BROADCAST_REQUEST_CODE = 0
        private const val ACTION_DISMISS_NOTIFICATION = "action_dismiss_notification"
        fun getPendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                BROADCAST_REQUEST_CODE,
                Intent(context, CastReceiver::class.java).apply {
                    action = ACTION_DISMISS_NOTIFICATION
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}