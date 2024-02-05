package com.sf.tadami.notifications.extensionsinstaller

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.sf.tadami.R
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.notifications.extensionsinstaller.ExtensionInstallerReceiver.Companion.openExtensionsPendingActivity
import com.sf.tadami.ui.themes.getNotificationsColor
import com.sf.tadami.utils.cancelNotification
import com.sf.tadami.utils.notify

class ExtensionInstallerNotifier(private val context: Context) {

    val updateNotification =
            NotificationCompat.Builder(context, Notifications.EXTENSIONS_UPDATES_CHANNEL).apply {
                setSmallIcon(R.drawable.ic_tada)
                setAutoCancel(false)
                setOngoing(true)
                setShowWhen(false)
                setContentTitle(context.getString(R.string.ext_install_service_notif))
                setProgress(100, 100, true)
                color = getNotificationsColor(context)
            }

    fun notify(context: Context, pkgName: String, action: String) {
        Intent(action).apply {
            data = Uri.parse("package:$pkgName")
            `package` = context.packageName
            context.sendBroadcast(this)
        }
    }

    fun notifyAdded(context: Context, pkgName: String) {
        notify(
            context,
            pkgName,
            ExtensionInstallerReceiver.ACTION_EXTENSION_ADDED
        )
    }

    fun notifyReplaced(context: Context, pkgName: String) {
        notify(
            context,
            pkgName,
            ExtensionInstallerReceiver.ACTION_EXTENSION_REPLACED
        )
    }

    fun notifyRemoved(context: Context, pkgName: String) {
        notify(
            context,
            pkgName,
            ExtensionInstallerReceiver.ACTION_EXTENSION_REMOVED
        )
    }

    fun promptUpdates(names: List<String>) {
        context.notify(
            Notifications.EXTENSIONS_UPDATES_ID,
            NotificationCompat.Builder(context, Notifications.EXTENSIONS_UPDATES_CHANNEL)
                .apply {
                    setContentTitle(
                        context.resources.getQuantityString(
                            R.plurals.update_check_notification_ext_updates,
                            names.size,
                            names.size,
                        ),
                    )
                    val extNames = names.joinToString(", ")
                    setContentText(extNames)
                    setStyle(NotificationCompat.BigTextStyle().bigText(extNames))
                    setSmallIcon(R.drawable.ic_extension)
                    setContentIntent(openExtensionsPendingActivity(context))
                    setAutoCancel(true)
                }.build()
        )
    }

    fun dismiss() {
        context.cancelNotification(Notifications.EXTENSIONS_UPDATES_ID)
    }
}
