package com.sf.tadami.notifications.download

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sf.tadami.data.download.TadamiDownloadManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** The actions of the download notifications. */
class DownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val downloadManager: TadamiDownloadManager = Injekt.get()
        // The manager updates the notifications
        when (intent?.action) {
            ACTION_PAUSE -> downloadManager.pauseDownloads()
            ACTION_RESUME -> downloadManager.startDownloads()
            ACTION_CANCEL -> downloadManager.clearQueue()
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.sf.tadami.download.PAUSE"
        const val ACTION_RESUME = "com.sf.tadami.download.RESUME"
        const val ACTION_CANCEL = "com.sf.tadami.download.CANCEL"

        fun getPendingIntent(context: Context, action: String): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                Intent(context, DownloadReceiver::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
