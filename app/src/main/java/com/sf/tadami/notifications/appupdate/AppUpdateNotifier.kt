package com.sf.tadami.notifications.appupdate

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.sf.tadami.R
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.utils.notificationManager

class AppUpdateNotifier(private val context: Context) {

    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }

    private val progressNotificationBuilder =
        NotificationCompat.Builder(context, Notifications.APP_UPDATE_DOWNLOAD_PROGRESS_CHANNEL).apply {
            setContentTitle(context.getString(R.string.app_name))
            setLargeIcon(notificationBitmap)
            color = context.getColor(R.color.midnightdusk_primary)
        }


    fun showProgressNotification(): Notification {
        return with(progressNotificationBuilder) {
            setContentText(context.getString(R.string.notification_downloading_app_update))
            setSmallIcon(android.R.drawable.stat_sys_download)
            setOngoing(true)
            clearActions()
            addAction(R.drawable.ic_close, context.getString(R.string.action_cancel), AppUpdateReceiver.getPendingIntent(context))
        }.build()

    }

    fun onProgressChange(progress: Int) {
        with(progressNotificationBuilder) {
            setProgress(100, progress, false)
            setOnlyAlertOnce(true)
        }
        progressNotificationBuilder.show()
    }

    private fun NotificationCompat.Builder.show(id: Int = Notifications.APP_UPDATE_DOWNLOAD_PROGRESS_ID) {
        context.notificationManager.notify(id, build())
    }

    fun showInstallNotification(apkUri : Uri) {
        val installIntent = getInstallApkIntent(apkUri)
        context.notificationManager.notify(
            Notifications.APP_UPDATE_DOWNLOAD_SUCCESS_ID,
            NotificationCompat.Builder(context, Notifications.APP_UPDATE_DOWNLOAD_SUCCESS_CHANNEL)
                .apply {
                    setContentTitle(context.getString(R.string.app_name))
                    setContentText(context.getString(R.string.notification_app_update_tap_install))
                    setSmallIcon(R.drawable.ic_tada)
                    setLargeIcon(notificationBitmap)
                    priority = NotificationCompat.PRIORITY_HIGH
                    color = context.getColor(R.color.midnightdusk_primary)
                    setContentIntent(installIntent)
                    setAutoCancel(true)
                    addAction(
                        R.drawable.ic_baseline_system_update,
                        context.getString(R.string.install),
                        installIntent,
                    )
                    addAction(
                        R.drawable.ic_close,
                        context.getString(R.string.action_cancel),
                        AppUpdateReceiver.dismissNotification(context,Notifications.APP_UPDATE_DOWNLOAD_SUCCESS_ID),
                    )
                }.build()
        )
    }

    fun cancelProgressNotification() {
        context.notificationManager.cancel(Notifications.APP_UPDATE_DOWNLOAD_PROGRESS_ID)
    }

    private fun getInstallApkIntent(apkUri : Uri): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri,"application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}