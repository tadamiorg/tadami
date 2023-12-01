package com.sf.tadami.notifications.libraryupdate

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sf.tadami.R
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.ui.main.MainActivity
import com.sf.tadami.ui.utils.chop
import com.sf.tadami.utils.notify

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
            color = context.getColor(R.color.midnightdusk_primary)
            addAction(R.drawable.ic_close, context.getString(R.string.action_cancel), cancelIntent)
        }
    }

    fun showProgressNotification(current: Int, total: Int) {
        progressNotificationBuilder.setContentTitle(
            context.getString(
                R.string.notification_updating_library,
                current,
                total
            )
        )

        context.notify(
            Notifications.LIBRARY_UPDATE_PROGRESS_ID,
            progressNotificationBuilder
                .setProgress(total, current, false)
                .build()
        )
    }

    fun showUpdateNotifications(updates: List<Pair<Anime, Array<Episode>>>) {
        context.notify(
            Notifications.LIBRARY_UPDATE_SUCCESS_ID,
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
                    color = context.getColor(R.color.midnightdusk_primary)
                    setContentIntent(getMainActivityIntent())
                    setAutoCancel(true)

                }.build()
        )
    }

    fun showFailureNotifications(animeNumber: Int, uriCompat: Uri) {
        if (animeNumber == 0) {
            return
        }
        val failedNotification =
            NotificationCompat.Builder(context, Notifications.LIBRARY_UPDATE_FAILURE_CHANNEL)
                .apply {
                    setContentTitle(
                        context.resources.getString(
                            R.string.notification_library_update_failed_result,
                            animeNumber
                        )
                    )
                    setContentText(context.getString(R.string.tap_see_details))
                    setSmallIcon(R.drawable.ic_tada)
                    color = context.getColor(R.color.midnightdusk_primary)
                    setContentIntent(getLogIntent(context, uriCompat))
                }.build()
        context.notify(Notifications.LIBRARY_UPDATE_FAILURE_ID, failedNotification)
    }

    fun showSkippedNotifications(animeNumber: Int,uriCompat: Uri) {
        if (animeNumber == 0) {
            return
        }

        val skippedNotification =
            NotificationCompat.Builder(context, Notifications.LIBRARY_UPDATE_SKIP_CHANNEL).apply {
                setContentTitle(
                    context.resources.getString(
                        R.string.notification_library_update_skipped_result,
                        animeNumber
                    )
                )
                color = context.getColor(R.color.midnightdusk_primary)
                setContentText(context.getString(R.string.tap_see_details))
                setSmallIcon(R.drawable.ic_tada)
                setContentIntent(getLogIntent(context, uriCompat))
            }.build()

        context.notify(Notifications.LIBRARY_UPDATE_SKIP_ID, skippedNotification)
    }

    fun cancelProgressNotification() {
        NotificationManagerCompat.from(context)
            .cancel(Notifications.LIBRARY_UPDATE_PROGRESS_ID)
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

    private fun getLogIntent(context: Context, uri: Uri): PendingIntent {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, "text/plain")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}

private const val NOTIF_TITLE_MAX_LEN = 45
