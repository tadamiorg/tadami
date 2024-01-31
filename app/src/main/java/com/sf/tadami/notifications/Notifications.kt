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
    const val LIBRARY_UPDATE_PROGRESS_ID = 100
    const val LIBRARY_UPDATE_SUCCESS_CHANNEL = "library_update_success_channel"
    const val LIBRARY_UPDATE_SUCCESS_ID = 101
    const val LIBRARY_UPDATE_FAILURE_CHANNEL = "library_update_failure_channel"
    const val LIBRARY_UPDATE_FAILURE_ID = 102
    const val LIBRARY_UPDATE_SKIP_CHANNEL = "library_update_skip_channel"
    const val LIBRARY_UPDATE_SKIP_ID = 103

    // App notifications
    const val APP_GROUP = "app_group"
    const val APP_UPDATE_DOWNLOAD_PROGRESS_CHANNEL = "app_update_download_progress_channel"
    const val APP_UPDATE_DOWNLOAD_PROGRESS_ID = 200
    const val APP_UPDATE_DOWNLOAD_SUCCESS_CHANNEL = "app_update_download_success_channel"
    const val APP_UPDATE_DOWNLOAD_SUCCESS_ID = 201

    // Cast notifications
    const val CAST_GROUP = "cast_group"
    const val CAST_PROXY_STATUS_CHANNEL = "cast_proxy_status_channel"
    const val CAST_PROXY_STATUS_ID = 300

    // Backup notifications
    const val BACKUP_RESTORE_GROUP= "backup_group"
    const val BACKUP_RESTORE_PROGRESS_CHANNEL="backup_restore_progress"
    const val RESTORE_PROGRESS_ID=400
    const val BACKUP_PROGRESS_ID=402
    const val BACKUP_RESTORE_COMPLETE_CHANNEL="backup_restore_complete"
    const val RESTORE_COMPLETE_ID=401
    const val BACKUP_COMPLETE_ID=403

    // Extensions installs and updates
    const val EXTENSIONS_GROUP= "extensions_group"
    const val EXTENSIONS_UPDATES_CHANNEL = "ext_apk_update_channel"
    const val EXTENSIONS_UPDATES_ID = 500
    const val EXTENSIONS_INSTALLER_ID = 501


    fun setupNotificationsChannels(context: Context) {
        val notificationService = NotificationManagerCompat.from(context)

        notificationService.createNotificationChannelGroupsCompat(
            listOf(
                NotificationChannelGroupCompat.Builder(APP_GROUP).apply {
                    setName(context.getString(R.string.notification_app_group))
                }.build(),
                NotificationChannelGroupCompat.Builder(EXTENSIONS_GROUP).apply {
                    setName(context.getString(R.string.notification_extensions_group))
                }.build(),
                NotificationChannelGroupCompat.Builder(LIBRARY_GROUP).apply {
                    setName(context.getString(R.string.notification_library_group))
                }.build(),
                NotificationChannelGroupCompat.Builder(CAST_GROUP).apply {
                    setName(context.getString(R.string.notification_cast_group))
                }.build(),
                NotificationChannelGroupCompat.Builder(BACKUP_RESTORE_GROUP).apply {
                    setName(context.getString(R.string.notification_backup_group))
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

        val extensionsGroupChannels = listOf(
            NotificationChannelCompat.Builder(
                EXTENSIONS_UPDATES_CHANNEL,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            ).apply {
                setGroup(EXTENSIONS_GROUP)
                setName(context.getString(R.string.channel_ext_updates))
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

        val backupChannels = listOf(
            NotificationChannelCompat.Builder(
                BACKUP_RESTORE_PROGRESS_CHANNEL,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setGroup(BACKUP_RESTORE_GROUP)
                setName(context.getString(R.string.notification_backup_progress))
            }.build(),
            NotificationChannelCompat.Builder(
                BACKUP_RESTORE_COMPLETE_CHANNEL,
                NotificationManagerCompat.IMPORTANCE_HIGH
            ).apply {
                setGroup(BACKUP_RESTORE_GROUP)
                setName(context.getString(R.string.notification_backup_complete))
            }.build()
        )


        notificationService.createNotificationChannelsCompat(
            appGroupChannels +
                    extensionsGroupChannels +
                    libraryGroupChannels +
                    castChannels +
                    backupChannels
        )
    }

}