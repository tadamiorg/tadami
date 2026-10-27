package com.sf.tadami.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.BackgroundJobs
import com.sf.tadami.data.backup.BackupCreator
import com.sf.tadami.data.backup.BackupOptions
import com.sf.tadami.data.backup.BackupRestoreListener
import com.sf.tadami.data.backup.BackupRestorer
import com.sf.tadami.data.backup.RestoreOptions
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateListener
import com.sf.tadami.notifications.libraryupdate.LibraryUpdateTarget
import com.sf.tadami.notifications.libraryupdate.LibraryUpdater
import com.sf.tadami.domain.storage.StorageManager
import com.sf.tadami.preferences.backup.BackupPreferences
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.backup_created
import com.sf.tadami.shared.resources.creating_backup_error
import com.sf.tadami.shared.resources.restore_completed
import com.sf.tadami.shared.resources.restoring_backup_error
import com.sf.tadami.shared.resources.notification_library_update_failed_result
import com.sf.tadami.shared.resources.notification_library_update_success_summary
import com.sf.tadami.ui.utils.UiToasts
import com.sf.tadami.utils.Log
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.openInBrowser
import com.sf.tadami.utils.pluralRes
import com.sf.tadami.utils.stringRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import kotlin.time.Duration.Companion.hours

/**
 * The jobs of the desktop app, run while it's open: they report with toasts what the Android workers
 * notify.
 */
class DesktopBackgroundJobs : BackgroundJobs {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var libraryUpdate: Job? = null
    private var scheduledUpdates: Job? = null
    private var backup: Job? = null
    private var scheduledBackups: Job? = null
    private var restore: Job? = null

    @Synchronized
    override fun startLibraryUpdate(categoryId: Long?, target: LibraryUpdateTarget): Boolean {
        if (libraryUpdate?.isActive == true) return false
        libraryUpdate = scope.launch {
            val preferences = Injekt.get<DataStore<Preferences>>().getPreferencesGroup(LibraryPreferences)
            try {
                LibraryUpdater(target, categoryId, AppDirs.cache, ToastsListener).run(preferences)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Library update failed", e)
            }
        }
        return true
    }

    /** Every [prefInterval] hours (the settings' when null) after the last update, while the app is open. */
    @Synchronized
    override fun scheduleLibraryUpdates(prefInterval: Int?) {
        scheduledUpdates?.cancel()
        scheduledUpdates = scope.launch {
            val dataStore = Injekt.get<DataStore<Preferences>>()
            while (isActive) {
                val preferences = dataStore.getPreferencesGroup(LibraryPreferences)
                val interval = (prefInterval ?: preferences.autoUpdateInterval).takeIf { it > 0 } ?: return@launch
                val next = preferences.lastUpdatedTimestamp + interval.hours.inWholeMilliseconds
                val wait = next - System.currentTimeMillis()
                if (wait > 0) {
                    delay(wait)
                } else {
                    startLibraryUpdate()
                    // Until it records its time
                    delay(MIN_RETRY_MS)
                }
            }
        }
    }

    @Synchronized
    override fun startBackup(uri: String, options: BackupOptions) {
        backup = scope.launch { createBackup(uri, options, isAutoBackup = false) }
    }

    private suspend fun createBackup(uri: String, options: BackupOptions, isAutoBackup: Boolean) {
        try {
            BackupCreator(isAutoBackup).createBackup(uri, options)
            if (!isAutoBackup) UiToasts.showToast(stringRes(Res.string.backup_created))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            UiToasts.showToast(stringRes(Res.string.creating_backup_error) + ": " + e.message, UiToasts.LENGTH_LONG)
        }
    }

    override fun isManualBackupRunning(): Boolean = backup?.isActive == true

    /** Every [prefInterval] hours (the settings' when null) after the last one, while the app is open. */
    @Synchronized
    override fun scheduleAutoBackups(prefInterval: Int?) {
        scheduledBackups?.cancel()
        scheduledBackups = scope.launch {
            val dataStore = Injekt.get<DataStore<Preferences>>()
            while (isActive) {
                val preferences = dataStore.getPreferencesGroup(BackupPreferences)
                val interval = (prefInterval ?: preferences.autoBackupInterval).takeIf { it > 0 } ?: return@launch
                val wait = preferences.autoBackupLastTimestamp + interval.hours.inWholeMilliseconds - System.currentTimeMillis()
                if (wait > 0) {
                    delay(wait)
                    continue
                }
                // Their own folder, so their rotation never deletes the manual backups
                val dir = Injekt.get<StorageManager>().getAutomaticBackupsDirectory()
                if (dir != null && !isRestoreRunning()) createBackup(dir.uri, BackupOptions(), isAutoBackup = true)
                // Until it records its time
                delay(MIN_RETRY_MS)
            }
        }
    }

    @Synchronized
    override fun startRestore(uri: String, options: RestoreOptions) {
        restore = scope.launch {
            try {
                BackupRestorer(AppDirs.cache, RestoreToasts).restore(uri, options)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                UiToasts.showToast(stringRes(Res.string.restoring_backup_error) + ": " + e.message, UiToasts.LENGTH_LONG)
            }
        }
    }

    override fun isRestoreRunning(): Boolean = restore?.isActive == true

    // The ad block is the in-app browser's, the desktop opens the user's
    override fun startAdBlockUpdate(manual: Boolean) = Unit

    override fun scheduleAdBlockUpdates(enabled: Boolean?) = Unit

    /** The release page: the user installs it. */
    override fun startAppUpdate(updateLink: String): Boolean {
        openInBrowser(updateLink)
        return true
    }

    private object ToastsListener : LibraryUpdateListener {
        override fun onUpdates(updates: List<Pair<Anime, Array<Episode>>>) {
            UiToasts.showToast(
                pluralRes(Res.plurals.notification_library_update_success_summary, updates.size, updates.size),
                UiToasts.LENGTH_LONG,
            )
        }

        override fun onFailures(count: Int, file: File) {
            UiToasts.showToast(stringRes(Res.string.notification_library_update_failed_result, count), UiToasts.LENGTH_LONG)
        }
    }

    private object RestoreToasts : BackupRestoreListener {
        override fun onRestoreProgress(content: String, contentTitle: String, progress: Int, maxAmount: Int) = Unit

        override fun onRestoreComplete(time: Long, errorCount: Int, path: String?, file: String?) {
            UiToasts.showToast(stringRes(Res.string.restore_completed), UiToasts.LENGTH_LONG)
        }
    }

    private companion object {
        const val TAG = "DesktopBackgroundJobs"
        const val MIN_RETRY_MS = 60_000L
    }
}
