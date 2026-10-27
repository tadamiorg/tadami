package com.sf.tadami

import android.app.Application
import androidx.core.net.toUri
import com.sf.tadami.data.backup.BackupOptions
import com.sf.tadami.data.backup.RestoreOptions
import com.sf.tadami.notifications.appupdate.AppUpdateWorker
import com.sf.tadami.notifications.backup.BackupCreateWorker
import com.sf.tadami.notifications.backup.BackupRestoreWorker
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateTarget
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateWorker
import com.sf.tadami.ui.webview.adblock.AdBlockUpdateWorker

/** The background jobs as WorkManager workers. */
class AndroidBackgroundJobs(private val app: Application) : BackgroundJobs {

    override fun startLibraryUpdate(categoryId: Long?, target: LibraryUpdateTarget): Boolean =
        LibraryUpdateWorker.startNow(app, categoryId, target)

    override fun scheduleLibraryUpdates(prefInterval: Int?) = LibraryUpdateWorker.setupTask(app, prefInterval)

    override fun startBackup(uri: String, options: BackupOptions) = BackupCreateWorker.startNow(app, uri.toUri(), options)

    override fun isManualBackupRunning(): Boolean = BackupCreateWorker.isManualJobRunning(app)

    override fun scheduleAutoBackups(prefInterval: Int?) = BackupCreateWorker.setupTask(app, prefInterval)

    override fun startRestore(uri: String, options: RestoreOptions) = BackupRestoreWorker.start(app, uri.toUri(), options)

    override fun isRestoreRunning(): Boolean = BackupRestoreWorker.isRunning(app)

    override fun startAdBlockUpdate(manual: Boolean) = AdBlockUpdateWorker.startNow(app, manual)

    override fun scheduleAdBlockUpdates(enabled: Boolean?) = AdBlockUpdateWorker.setupTask(app, enabled)

    override fun startAppUpdate(updateLink: String): Boolean = AppUpdateWorker.startNow(app, updateLink)
}
