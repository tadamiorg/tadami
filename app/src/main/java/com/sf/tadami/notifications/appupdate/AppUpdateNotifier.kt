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
import com.sf.tadami.ui.themes.getNotificationsColor
import com.sf.tadami.utils.notificationManager

class AppUpdateNotifier(private val context: Context) {

    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }

    private val progressNotificationBuilder =
        NotificationCompat.Builder(context, Notifications.APP_UPDATE_DOWNLOAD_PROGRESS_CHANNEL).apply {
            setContentTitle(context.getString(R.string.app_name))
            setLargeIcon(notificationBitmap)
            color = getNotificationsColor(context)
        }


    fun showProgressNotification(title: String? = null): Notification {
        return with(progressNotificationBuilder) {
            title?.let { setContentTitle(title) }
            setContentText(context.getString(R.string.notification_downloading_app_update))
            setSmallIcon(android.R.drawable.stat_sys_download)
            setOngoing(true)
            clearActions()
            addAction(R.drawable.ic_close, context.getString(R.string.action_cancel), AppUpdateReceiver.cancelDownloadAppUpdatePendingBroadcast(context))
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

    fun promptInstall(apkUri : Uri) {
        val installIntent = getInstallApkIntent(apkUri,context)
        context.notificationManager.notify(
            Notifications.APP_UPDATE_DOWNLOAD_SUCCESS_ID,
            NotificationCompat.Builder(context, Notifications.APP_UPDATE_DOWNLOAD_SUCCESS_CHANNEL)
                .apply {
                    setContentText(context.getString(R.string.notification_app_update_tap_install))
                    setSmallIcon(android.R.drawable.stat_sys_download_done)
                    setOnlyAlertOnce(false)
                    setProgress(0, 0, false)
                    setContentIntent(installIntent)
                    setOngoing(true)
                    color = getNotificationsColor(context)

                    clearActions()
                    addAction(
                        R.drawable.ic_baseline_system_update,
                        context.getString(R.string.install),
                        installIntent,
                    )
                    addAction(
                        R.drawable.ic_close,
                        context.getString(R.string.action_cancel),
                        AppUpdateReceiver.dismissNotificationPendingBroadcast(context,Notifications.APP_UPDATE_DOWNLOAD_SUCCESS_ID),
                    )
                }.build()
        )
    }

    fun onDownloadError(url: String) {
        context.notificationManager.notify(
            Notifications.APP_UPDATE_DOWNLOAD_ERROR_ID,
            NotificationCompat.Builder(context, Notifications.APP_UPDATE_DOWNLOAD_ERROR_CHANNEL)
                .apply {
                    setContentText(context.getString(R.string.update_check_notification_download_error))
                    setSmallIcon(R.drawable.ic_warning_white)
                    setOnlyAlertOnce(false)
                    setProgress(0, 0, false)

                    clearActions()
                    addAction(
                        R.drawable.ic_refresh,
                        context.getString(R.string.retry),
                        AppUpdateReceiver.downloadAppUpdatePendingBroadcast(context, url,  notificationId = Notifications.APP_UPDATE_DOWNLOAD_ERROR_ID),
                    )
                    addAction(
                        R.drawable.ic_close,
                        context.getString(R.string.action_cancel),
                        AppUpdateReceiver.dismissNotificationPendingBroadcast(context, Notifications.APP_UPDATE_DOWNLOAD_ERROR_ID),
                    )
                }.build()
        )
    }

    fun cancel() {
        AppUpdateReceiver.dismissNotification(context, Notifications.APP_UPDATE_DOWNLOAD_PROGRESS_ID)
    }


    private fun getInstallApkIntent(apkUri : Uri,context: Context): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri,"application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}