package com.sf.tadami.notifications.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hippo.unifile.UniFile
import com.sf.tadami.data.backup.BackupCreateFlags
import com.sf.tadami.data.backup.BackupCreator
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.preferences.backup.BackupPreferences
import com.sf.tadami.utils.cancelNotification
import com.sf.tadami.utils.editPreferences
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.isRunning
import com.sf.tadami.utils.workManager
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class BackupCreateWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val notifier = BackupNotifier(context)

    override suspend fun doWork(): Result {
        val isAutoBackup = inputData.getBoolean(IS_AUTO_BACKUP_KEY, true)
        val dataStore : DataStore<Preferences> = Injekt.get()
        val backupPreferences = dataStore.getPreferencesGroup(BackupPreferences)

        if (isAutoBackup && BackupRestoreWorker.isRunning(context)) return Result.retry()

        val uri = inputData.getString(LOCATION_URI_KEY)?.toUri()
            ?: backupPreferences.autoBackupFolder.toUri()
        val flags = inputData.getInt(BACKUP_FLAGS_KEY, BackupCreateFlags.AutomaticDefaults)

        try {
            setForeground(getForegroundInfo())
        } catch (e: IllegalStateException) {
            Log.e("BackupCreateWorker","Not allowed to run on foreground service")
        }
        return try {
            val location = BackupCreator(context).createBackup(uri, flags, isAutoBackup)
            if (isAutoBackup) {
                dataStore.editPreferences(backupPreferences.copy(autoBackupLastTimestamp = Date().time),
                    BackupPreferences
                )
            } else {
                notifier.showBackupComplete(UniFile.fromUri(context,location.toUri()))
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("BackupCreateWorker",e.stackTraceToString())
            if (!isAutoBackup) notifier.showBackupError(e.message)
            Result.failure()
        }
        finally {
            context.cancelNotification(Notifications.BACKUP_PROGRESS_ID)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.BACKUP_PROGRESS_ID,
            notifier.showBackupProgress().build(),
        )
    }

    companion object {
        fun isManualJobRunning(context: Context): Boolean {
            return context.workManager.isRunning(TAG_MANUAL)
        }

        fun setupTask(context: Context, prefInterval: Int? = null) {
            val dataStore : DataStore<Preferences> = Injekt.get()
            val backupPreferences = runBlocking { dataStore.getPreferencesGroup(BackupPreferences) }
            val interval = prefInterval ?: backupPreferences.autoBackupInterval
            if (interval > 0) {
                val constraints = Constraints(
                    requiresBatteryNotLow = true,
                )

                val request = PeriodicWorkRequestBuilder<BackupCreateWorker>(
                    interval.toLong(),
                    TimeUnit.HOURS,
                    10,
                    TimeUnit.MINUTES,
                )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10.minutes.toJavaDuration())
                    .addTag(TAG_AUTO)
                    .setConstraints(constraints)
                    .setInputData(workDataOf(IS_AUTO_BACKUP_KEY to true))
                    .build()

                context.workManager.enqueueUniquePeriodicWork(TAG_AUTO, ExistingPeriodicWorkPolicy.UPDATE, request)
            } else {
                context.workManager.cancelUniqueWork(TAG_AUTO)
            }
        }

        fun startNow(context: Context, uri: Uri, flags: Int) {
            val inputData = workDataOf(
                IS_AUTO_BACKUP_KEY to false,
                LOCATION_URI_KEY to uri.toString(),
                BACKUP_FLAGS_KEY to flags,
            )
            val request = OneTimeWorkRequestBuilder<BackupCreateWorker>()
                .addTag(TAG_MANUAL)
                .setInputData(inputData)
                .build()
            context.workManager.enqueueUniqueWork(TAG_MANUAL, ExistingWorkPolicy.KEEP, request)
        }
    }
}

private const val TAG_AUTO = "BackupCreator"
private const val TAG_MANUAL = "$TAG_AUTO:manual"

private const val IS_AUTO_BACKUP_KEY = "is_auto_backup" // Boolean
private const val LOCATION_URI_KEY = "location_uri" // String
private const val BACKUP_FLAGS_KEY = "backup_flags" // Int
