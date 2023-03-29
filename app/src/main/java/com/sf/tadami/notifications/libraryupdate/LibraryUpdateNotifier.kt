package com.sf.tadami.notifications.libraryupdate

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.ui.main.MainActivity
import com.sf.tadami.ui.utils.chop
import com.sf.tadami.utils.notificationManager

class LibraryUpdateNotifier(private val context: Context) {

    private val cancelIntent = LibraryUpdateReceiver.getPendingIntent(context)

    private val notificationBitmap by lazy {
        BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
    }

    val progressNotificationBuilder by lazy {
        NotificationCompat.Builder(context, Notifications.LIBRARY_UPDATE_PROGRESS_CHANNEL).apply {
            setContentTitle(context.getString(R.string.app_name))
            setSmallIcon(R.drawable.ic_refresh)
            setLargeIcon(notificationBitmap)
            setOngoing(true)
            setOnlyAlertOnce(true)
            addAction(R.drawable.ic_close, context.getString(R.string.action_cancel), cancelIntent)
        }
    }

    fun showProgressNotification(current: Int, total: Int) {
        progressNotificationBuilder.setContentTitle(
            context.getString(
                R.string.notification_updating,
                current,
                total
            )
        )

        context.notificationManager.notify(
            Notifications.LIBRARY_UPDATE_PROGRESS_NOTIFICATION,
            progressNotificationBuilder
                .setProgress(total, current, false)
                .build(),
        )
    }

    fun showUpdateNotifications(updates: List<Pair<Anime, Array<Episode>>>) {
        context.notificationManager.notify(
            Notifications.LIBRARY_UPDATE_SUCCESS_NOTIFICATION,
            NotificationCompat.Builder(context, Notifications.LIBRARY_UPDATE_SUCCESS_CHANNEL)
                .apply {
                    setContentTitle(context.getString(R.string.notification_library_update_success_title))
                    if (updates.size == 1) {
                        setContentText(updates.first().first.title.chop(NOTIF_TITLE_MAX_LEN))
                    } else {
                        setContentText(
                            context.resources.getQuantityString(
                                R.plurals.notification_library_update_success_summary,
                                updates.size,
                                updates.size
                            )
                        )


                        setStyle(
                            NotificationCompat.BigTextStyle().bigText(
                                updates.joinToString("\n") {
                                    it.first.title.chop(NOTIF_TITLE_MAX_LEN)
                                },
                            ),
                        )

                    }

                    setSmallIcon(R.drawable.ic_tada)
                    setLargeIcon(notificationBitmap)

                    setGroup(Notifications.LIBRARY_GROUP)
                    setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                    setGroupSummary(true)
                    priority = NotificationCompat.PRIORITY_HIGH

                    setContentIntent(getMainActivityIntent())
                    setAutoCancel(true)

                }.build()
        )
    }

    fun showFailureNotifications(animeNumber: Int, uriCompat: Any) {
        context.notificationManager.notify(
            Notifications.LIBRARY_UPDATE_FAILURE_NOTIFICATION,
            NotificationCompat.Builder(context, Notifications.LIBRARY_UPDATE_FAILURE_CHANNEL)
                .apply {

                }.build()
        )
    }

    fun cancelProgressNotification() {
        context.notificationManager.cancel(Notifications.LIBRARY_UPDATE_PROGRESS_NOTIFICATION)
    }

    private fun getMainActivityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

private const val NOTIF_MAX_CHAPTERS = 5
private const val NOTIF_TITLE_MAX_LEN = 45
private const val NOTIF_ICON_SIZE = 192