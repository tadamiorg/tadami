package com.sf.tadami.notifications.download

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sf.tadami.R
import com.sf.tadami.data.download.model.Download
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.ui.main.MainActivity
import com.sf.tadami.ui.themes.getNotificationsColor
import com.sf.tadami.ui.utils.chop
import com.sf.tadami.utils.cancelNotification
import com.sf.tadami.utils.notify

class DownloadNotifier(private val context: Context) {

    val progressNotificationBuilder by lazy {
        NotificationCompat.Builder(context, Notifications.DOWNLOADER_PROGRESS_CHANNEL).apply {
            setContentTitle(context.getString(R.string.notification_download_waiting))
            setSmallIcon(android.R.drawable.stat_sys_download)
            setOngoing(true)
            setOnlyAlertOnce(true)
            setShowWhen(false)
            color = getNotificationsColor(context)
            setContentIntent(mainActivityIntent())
            addAction(
                R.drawable.ic_pause_24,
                context.getString(R.string.action_pause),
                DownloadReceiver.getPendingIntent(context, DownloadReceiver.ACTION_PAUSE),
            )
            addAction(
                R.drawable.ic_close,
                context.getString(R.string.action_cancel),
                DownloadReceiver.getPendingIntent(context, DownloadReceiver.ACTION_CANCEL),
            )
        }
    }

    /** The downloads in progress, [queued] counting those waiting too. */
    fun showProgress(active: List<Download>, queued: Int) {
        progressNotificationBuilder.apply {
            when (active.size) {
                0 -> {
                    setContentTitle(context.getString(R.string.notification_download_waiting))
                    setContentText(null)
                    setProgress(0, 0, true)
                }
                1 -> {
                    val download = active.first()
                    setContentTitle(download.anime.title.chop(TITLE_MAX_LENGTH))
                    setContentText(download.episode.name.chop(TITLE_MAX_LENGTH))
                    setProgress(100, download.progress, false)
                }
                else -> {
                    setContentTitle(
                        context.resources.getQuantityString(R.plurals.notification_downloading_episodes, active.size, active.size),
                    )
                    setContentText(active.joinToString { it.episode.name }.chop(TITLE_MAX_LENGTH * 2))
                    setProgress(100, active.sumOf { it.progress } / active.size, false)
                }
            }
            setSubText(
                context.resources.getQuantityString(R.plurals.notification_download_queued, queued, queued),
            )
        }
        context.notify(Notifications.DOWNLOADER_PROGRESS_ID, progressNotificationBuilder.build())
    }

    fun dismissProgress() {
        context.cancelNotification(Notifications.DOWNLOADER_PROGRESS_ID)
    }

    fun showPaused(queued: Int) {
        val notification = NotificationCompat.Builder(context, Notifications.DOWNLOADER_PROGRESS_CHANNEL).apply {
            setContentTitle(context.getString(R.string.notification_download_paused))
            setContentText(
                context.resources.getQuantityString(R.plurals.notification_download_queued, queued, queued),
            )
            setSmallIcon(R.drawable.ic_pause_24)
            setOnlyAlertOnce(true)
            color = getNotificationsColor(context)
            setContentIntent(mainActivityIntent())
            addAction(
                R.drawable.ic_play_24,
                context.getString(R.string.action_resume),
                DownloadReceiver.getPendingIntent(context, DownloadReceiver.ACTION_RESUME),
            )
            addAction(
                R.drawable.ic_close,
                context.getString(R.string.action_cancel),
                DownloadReceiver.getPendingIntent(context, DownloadReceiver.ACTION_CANCEL),
            )
        }.build()
        context.notify(Notifications.DOWNLOADER_PAUSED_ID, notification)
    }

    fun dismissPaused() {
        context.cancelNotification(Notifications.DOWNLOADER_PAUSED_ID)
    }

    fun showError(download: Download) {
        val notification = NotificationCompat.Builder(context, Notifications.DOWNLOADER_ERROR_CHANNEL).apply {
            setContentTitle(
                context.getString(R.string.notification_download_error, download.episode.name.chop(TITLE_MAX_LENGTH)),
            )
            setContentText(download.error)
            setStyle(NotificationCompat.BigTextStyle().bigText("${download.anime.title}\n${download.error.orEmpty()}"))
            setSmallIcon(android.R.drawable.stat_sys_warning)
            color = getNotificationsColor(context)
            setContentIntent(mainActivityIntent())
            setAutoCancel(true)
        }.build()
        context.notify(Notifications.DOWNLOADER_ERROR_ID, notification)
    }

    private fun mainActivityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_DOWNLOADS
            // Single top: the running activity gets the intent instead of being recreated
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val TITLE_MAX_LENGTH = 45
    }
}
