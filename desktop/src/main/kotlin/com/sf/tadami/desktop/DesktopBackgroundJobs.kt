package com.sf.tadami.desktop

import com.sf.tadami.BackgroundJobs
import com.sf.tadami.data.backup.BackupOptions
import com.sf.tadami.data.backup.RestoreOptions
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateTarget
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.utils.openInBrowser

/**
 * The jobs of the desktop app, run while it's open. The library update and the backups still run in
 * Android workers only: not there yet.
 */
class DesktopBackgroundJobs : BackgroundJobs {

    private fun notYet(): Boolean {
        UiToasts.showToast("Not available on the desktop yet")
        return false
    }

    override fun startLibraryUpdate(categoryId: Long?, target: LibraryUpdateTarget): Boolean = notYet()

    override fun scheduleLibraryUpdates(prefInterval: Int?) = Unit

    override fun startBackup(uri: String, options: BackupOptions) {
        notYet()
    }

    override fun isManualBackupRunning(): Boolean = false

    override fun scheduleAutoBackups(prefInterval: Int?) = Unit

    override fun startRestore(uri: String, options: RestoreOptions) {
        notYet()
    }

    override fun isRestoreRunning(): Boolean = false

    // The ad block is the in-app browser's, the desktop opens the user's
    override fun startAdBlockUpdate(manual: Boolean) = Unit

    override fun scheduleAdBlockUpdates(enabled: Boolean?) = Unit

    /** The release page: the user installs it. */
    override fun startAppUpdate(updateLink: String): Boolean {
        openInBrowser(updateLink)
        return true
    }
}
