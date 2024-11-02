package com.sf.tadami.ui.tabs.more.settings.screens.data

import android.content.Context
import android.net.Uri
import com.sf.tadami.notifications.backup.BackupCreateWorker

class BackupCreateUtils {
    companion object {
        fun createBackup(context: Context, uri: Uri,flags : Int) {
            BackupCreateWorker.startNow(context, uri, flags)
        }
    }
}