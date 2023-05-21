package com.sf.tadami.notifications.cast

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.sf.tadami.R
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.ui.main.MainActivity

class CastNotifier(private val context: Context) {

    private val cancelIntent = CastReceiver.getPendingIntent(context)

    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }

    val castStatusNotificationBuilder by lazy {
        NotificationCompat.Builder(context, Notifications.CAST_PROXY_STATUS_CHANNEL).apply {
            setContentTitle(context.getString(R.string.app_name))
            setContentIntent(getMainActivityIntent())
            setSmallIcon(R.drawable.ic_tada)
            setLargeIcon(notificationBitmap)
            setOngoing(true)
            setContentText(context.getString(R.string.notification_cast_status_text))
            setOnlyAlertOnce(true)
            addAction(R.drawable.ic_close, context.getString(R.string.action_cancel), cancelIntent)
        }
    }

    private fun getMainActivityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
