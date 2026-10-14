package com.sf.tadami.notifications.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import com.hippo.unifile.UniFile
import com.sf.tadami.R
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.ui.main.MainActivity
import com.sf.tadami.ui.utils.getUriCompat
import com.sf.tadami.utils.cancelNotification
import com.sf.tadami.utils.notify
import java.io.File
import java.util.concurrent.TimeUnit

class BackupNotifier(private val context: Context) {

    private val progressNotificationBuilder = NotificationCompat.Builder(context, Notifications.BACKUP_RESTORE_PROGRESS_CHANNEL).apply {
        setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
        setSmallIcon(R.drawable.ic_tada)
        setAutoCancel(false)
        setOngoing(true)
        setOnlyAlertOnce(true)
    }

    private val completeNotificationBuilder = NotificationCompat.Builder(context, Notifications.BACKUP_RESTORE_COMPLETE_CHANNEL).apply {
        setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
        setSmallIcon(R.drawable.ic_tada)
        setAutoCancel(false)
    }

    private fun NotificationCompat.Builder.show(id: Int) {
        context.notify(id, build())
    }

    fun showBackupProgress(): NotificationCompat.Builder {
        val builder = with(progressNotificationBuilder) {
            setContentTitle(context.getString(R.string.creating_backup))
            setProgress(0,0,true)
            setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder
    }

    fun showBackupError(error: String?) {
        context.cancelNotification(Notifications.BACKUP_PROGRESS_ID)

        with(completeNotificationBuilder) {
            setContentTitle(context.getString(R.string.creating_backup_error))
            setContentText(error)
        }.show(Notifications.BACKUP_COMPLETE_ID)
    }

    /** Opens the backup settings on tap, where the automatic backups can be fixed. */
    fun showAutoBackupError(error: String?) {
        context.cancelNotification(Notifications.BACKUP_PROGRESS_ID)

        NotificationCompat.Builder(context, Notifications.BACKUP_RESTORE_COMPLETE_CHANNEL).apply {
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            setSmallIcon(R.drawable.ic_tada)
            setContentTitle(context.getString(R.string.creating_auto_backup_error))
            setContentText(error)
            setStyle(NotificationCompat.BigTextStyle().bigText(error))
            setContentIntent(openBackupSettingsPendingActivity())
            setAutoCancel(true)
        }.show(Notifications.AUTO_BACKUP_ERROR_ID)
    }

    private fun openBackupSettingsPendingActivity(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_BACKUP_SETTINGS
            // Single top: the running activity gets the intent instead of being recreated
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun showBackupComplete(unifile: UniFile) {
        context.cancelNotification(Notifications.BACKUP_PROGRESS_ID)

        with(completeNotificationBuilder) {
            setContentTitle(context.getString(R.string.backup_created))
            setContentText(unifile.filePath ?: unifile.name)

            clearActions()
            addAction(
                R.drawable.ic_share,
                context.getString(R.string.action_share),
                BackupReceiver.shareBackupPendingActivity(context, unifile.uri),
            )
        }.show(Notifications.BACKUP_COMPLETE_ID)
    }

    fun showRestoreProgress(
        content: String = "",
        contentTitle: String = context.getString(R.string.restoring_backup),
        progress: Int = 0,
        maxAmount: Int = 100,
    ): NotificationCompat.Builder {
        val builder = with(progressNotificationBuilder) {
            setContentTitle(contentTitle)


            setContentText(content)
            setProgress(maxAmount, progress, false)
            setOnlyAlertOnce(true)

            clearActions()
            addAction(
                R.drawable.ic_close,
                context.getString(R.string.action_cancel),
                BackupReceiver.cancelRestorePendingBroadcast(context, Notifications.RESTORE_PROGRESS_ID),
            )
        }

        builder.show(Notifications.RESTORE_PROGRESS_ID)

        return builder
    }

    fun showRestoreError(error: String?) {
        context.cancelNotification(Notifications.RESTORE_PROGRESS_ID)

        with(completeNotificationBuilder) {
            setContentTitle(context.getString(R.string.restoring_backup_error))
            setContentText(error)

            show(Notifications.RESTORE_COMPLETE_ID)
        }
    }

    fun showRestoreComplete(
        time: Long,
        errorCount: Int,
        path: String?,
        file: String?,
        contentTitle: String = context.getString(R.string.restore_completed),
    ) {
        context.cancelNotification(Notifications.RESTORE_PROGRESS_ID)

        val timeString = context.getString(
            R.string.restore_duration,
            TimeUnit.MILLISECONDS.toMinutes(time),
            TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(time),
            ),
        )

        with(completeNotificationBuilder) {
            setContentTitle(contentTitle)
            setContentText(
                context.resources.getQuantityString(
                    R.plurals.restore_completed_message,
                    errorCount,
                    timeString,
                    errorCount,
                ),
            )

            clearActions()
            if (errorCount > 0 && !path.isNullOrEmpty() && !file.isNullOrEmpty()) {
                val destFile = File(path, file)
                val uri = destFile.getUriCompat(context)

                val errorLogIntent = BackupReceiver.openErrorLogPendingActivity(context, uri)
                setContentIntent(errorLogIntent)
                addAction(
                    R.drawable.ic_folder,
                    context.getString(R.string.tap_see_details),
                    errorLogIntent,
                )
            }

            show(Notifications.RESTORE_COMPLETE_ID)
        }
    }
}