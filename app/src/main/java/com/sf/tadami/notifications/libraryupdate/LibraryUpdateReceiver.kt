package com.sf.tadami.notifications.libraryupdate

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LibraryUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        LibraryUpdateWorker.stop(context)
    }

    companion object{
        private const val BROADCAST_REQUEST_CODE = 0
        fun getPendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                BROADCAST_REQUEST_CODE,
                Intent(context, LibraryUpdateReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}