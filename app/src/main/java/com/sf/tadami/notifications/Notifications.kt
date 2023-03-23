package com.sf.tadami.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import com.sf.tadami.R

object Notifications {

    // Library notifications
    const val LIBRARY_GROUP = "library_group"
    const val LIBRARY_UPDATE_PROGRESS_CHANNEL = "library_update_progress_channel"
    const val LIBRARY_UPDATE_PROGRESS_NOTIFICATION = 100
    const val LIBRARY_UPDATE_SUCCESS_CHANNEL = "library_update_success_channel"
    const val LIBRARY_UPDATE_SUCCESS_NOTIFICATION = 101
    const val LIBRARY_UPDATE_FAILURE_CHANNEL = "library_update_failure_channel"
    const val LIBRARY_UPDATE_FAILURE_NOTIFICATION = 102


    fun setupNotificationsChannels(context: Context) {
        val notificationService = NotificationManagerCompat.from(context)

        notificationService.createNotificationChannelGroupsCompat(
            listOf(
                NotificationChannelGroupCompat.Builder(LIBRARY_GROUP).apply {
                    setName(context.getString(R.string.notification_library_group))
                }.build()
            )
        )

        notificationService.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(
                    LIBRARY_UPDATE_PROGRESS_CHANNEL,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT
                ).apply {
                    setGroup(LIBRARY_GROUP)
                    setName(context.getString(R.string.notification_library_updates))
                }.build(),
                NotificationChannelCompat.Builder(
                    LIBRARY_UPDATE_SUCCESS_CHANNEL,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT
                ).apply {
                    setGroup(LIBRARY_GROUP)
                    setName(context.getString(R.string.notification_library_updates))
                }.build(),
                NotificationChannelCompat.Builder(
                    LIBRARY_UPDATE_FAILURE_CHANNEL,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT
                ).apply {
                    setGroup(LIBRARY_GROUP)
                    setName(context.getString(R.string.notification_library_updates))
                }.build()
            )
        )
    }

}