package com.sf.tadami.notifications.extensionsinstaller

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sf.tadami.R
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.ui.main.MainActivity
import com.sf.tadami.utils.cancelNotification
import com.sf.tadami.utils.notify

class ExtensionInstallerNotifier(private val context: Context) {

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
                    setContentIntent(openExtensionsPendingActivity())
                    setAutoCancel(true)
                }.build()
        )
    }

    fun dismiss() {
        context.cancelNotification(Notifications.EXTENSIONS_UPDATES_ID)
    }

    private fun openExtensionsPendingActivity(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_EXTENSIONS
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
}
