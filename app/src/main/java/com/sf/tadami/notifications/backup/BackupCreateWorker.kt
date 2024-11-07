package com.sf.tadami.notifications.backup

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
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
import com.sf.tadami.data.backup.BackupCreator
import com.sf.tadami.data.backup.BackupOptions
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.preferences.backup.BackupPreferences
import com.sf.tadami.preferences.storage.StoragePreferences
import com.sf.tadami.utils.cancelNotification
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.isRunning
import com.sf.tadami.utils.workManager
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class BackupCreateWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val notifier = BackupNotifier(context)

    override suspend fun doWork(): Result {
        val isAutoBackup = inputData.getBoolean(IS_AUTO_BACKUP_KEY, true)
        val dataStore : DataStore<Preferences> = Injekt.get()
        val storagePreferences = dataStore.getPreferencesGroup(StoragePreferences)

        if (isAutoBackup && BackupRestoreWorker.isRunning(context)) return Result.retry()

        val uri = inputData.getString(LOCATION_URI_KEY)?.toUri()
            ?: storagePreferences.storageDir.toUri()
        val options = inputData.getBooleanArray(OPTIONS_KEY)?.let { BackupOptions.fromBooleanArray(it) }
            ?: BackupOptions()

        try {
            setForeground(getForegroundInfo())
        } catch (e: IllegalStateException) {
            Log.e("BackupCreateWorker","Not allowed to run on foreground service")
        }
        return try {
            val location = BackupCreator(context,isAutoBackup).createBackup(uri, options)
            if (!isAutoBackup) {
                notifier.showBackupComplete(UniFile.fromUri(context,location.toUri())!!)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
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

        fun startNow(context: Context, uri: Uri, options: BackupOptions) {
            val inputData = workDataOf(
                IS_AUTO_BACKUP_KEY to false,
                LOCATION_URI_KEY to uri.toString(),
                OPTIONS_KEY to options.asBooleanArray(),
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
private const val OPTIONS_KEY = "options" // BooleanArray
