package com.sf.tadami

import com.sf.tadami.data.backup.BackupOptions
import com.sf.tadami.data.backup.RestoreOptions
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateTarget

/**
 * The jobs that go on without the screen that started them, registered in Injekt: WorkManager
 * workers on Android.
 */
interface BackgroundJobs {

    /**
     * Updates the library, the category [categoryId] only when given.
     *
     * @return false when an update is already running.
     */
    fun startLibraryUpdate(categoryId: Long? = null, target: LibraryUpdateTarget = LibraryUpdateTarget.EPISODES): Boolean

    /** Schedules the periodic library update, every [prefInterval] hours, the settings' when null. */
    fun scheduleLibraryUpdates(prefInterval: Int? = null)

    /** Backs the library up into [uri]. */
    fun startBackup(uri: String, options: BackupOptions)

    /** Whether a backup the user asked for is running. */
    fun isManualBackupRunning(): Boolean

    /** Schedules the automatic backups, every [prefInterval] hours, the settings' when null. */
    fun scheduleAutoBackups(prefInterval: Int? = null)

    /** Restores the backup at [uri]. */
    fun startRestore(uri: String, options: RestoreOptions)

    fun isRestoreRunning(): Boolean

    /** Updates the ad block lists of the in-app browser, [manual] when the user asked for it. */
    fun startAdBlockUpdate(manual: Boolean)

    /** Schedules the update of the ad block lists, or cancels it when they're not [enabled]. */
    fun scheduleAdBlockUpdates(enabled: Boolean? = null)

    /**
     * Downloads the app update at [updateLink] and installs it.
     *
     * @return false when one is already downloading.
     */
    fun startAppUpdate(updateLink: String): Boolean
}
