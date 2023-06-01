package com.sf.tadami.notifications

import android.app.NotificationManager
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
    const val LIBRARY_UPDATE_SKIP_CHANNEL = "library_update_skip_channel"
    const val LIBRARY_UPDATE_SKIP_NOTIFICATION = 103

    // App notifications
    const val APP_GROUP = "app_group"
    const val APP_UPDATE_DOWNLOAD_PROGRESS_CHANNEL = "app_update_download_progress_channel"
    const val APP_UPDATE_DOWNLOAD_PROGRESS_NOTIFICATION = 200
    const val APP_UPDATE_DOWNLOAD_SUCCESS_CHANNEL = "app_update_download_success_channel"
    const val APP_UPDATE_DOWNLOAD_SUCCESS_NOTIFICATION = 201

    // Cast notifications
    const val CAST_GROUP = "cast_group"
    const val CAST_PROXY_STATUS_CHANNEL = "cast_proxy_status_channel"
    const val CAST_PROXY_STATUS_NOTIFICATION = 300


    fun setupNotificationsChannels(context: Context) {
        val notificationService = NotificationManagerCompat.from(context)

        notificationService.createNotificationChannelGroupsCompat(
            listOf(
                NotificationChannelGroupCompat.Builder(APP_GROUP).apply {
                    setName(context.getString(R.string.notification_app_group))
                }.build(),
                NotificationChannelGroupCompat.Builder(LIBRARY_GROUP).apply {
                    setName(context.getString(R.string.notification_library_group))
                }.build(),
                NotificationChannelGroupCompat.Builder(CAST_GROUP).apply {
                    setName(context.getString(R.string.notification_cast_group))
                }.build()
            )
        )

        val appGroupChannels = listOf(
            NotificationChannelCompat.Builder(
                APP_UPDATE_DOWNLOAD_PROGRESS_CHANNEL,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            ).apply {
                setGroup(APP_GROUP)
                setName(context.getString(R.string.notification_app_update_progress))
            }.build(),
            NotificationChannelCompat.Builder(
                APP_UPDATE_DOWNLOAD_SUCCESS_CHANNEL,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            ).apply {
                setGroup(APP_GROUP)
                setName(context.getString(R.string.notification_app_update_success))
            }.build()
        )

        val libraryGroupChannels = listOf(
            NotificationChannelCompat.Builder(
                LIBRARY_UPDATE_PROGRESS_CHANNEL,
                NotificationManagerCompat.IMPORTANCE_LOW
            ).apply {
                setGroup(LIBRARY_GROUP)
                setName(context.getString(R.string.notification_library_updates_progress))
            }.build(),
            NotificationChannelCompat.Builder(
                LIBRARY_UPDATE_SUCCESS_CHANNEL,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            ).apply {
                setGroup(LIBRARY_GROUP)
                setName(context.getString(R.string.notification_library_updates_sucess))
            }.build(),
            NotificationChannelCompat.Builder(
                LIBRARY_UPDATE_FAILURE_CHANNEL,
                NotificationManagerCompat.IMPORTANCE_LOW
            ).apply {
                setGroup(LIBRARY_GROUP)
                setName(context.getString(R.string.notification_library_updates_failure))
            }.build(),
            NotificationChannelCompat.Builder(
                LIBRARY_UPDATE_SKIP_CHANNEL,
                NotificationManagerCompat.IMPORTANCE_LOW
            ).apply {
                setGroup(LIBRARY_GROUP)
                setName(context.getString(R.string.notification_library_updates_skipped))
            }.build()
        )

        val castChannels = listOf(
            NotificationChannelCompat.Builder(
                CAST_PROXY_STATUS_CHANNEL,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setGroup(CAST_GROUP)
                setName(context.getString(R.string.notification_cast_proxy_status))
            }.build()
        )

        notificationService.createNotificationChannelsCompat(
            appGroupChannels +
                    libraryGroupChannels +
                    castChannels
        )
    }

}