package com.sf.tadami.notifications.backup

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sf.tadami.R
import com.sf.tadami.data.backup.BackupRestorer
import com.sf.tadami.data.backup.RestoreOptions
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.utils.cancelNotification
import com.sf.tadami.utils.isRunning
import com.sf.tadami.utils.workManager
import java.util.concurrent.CancellationException

class BackupRestoreWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val notifier = BackupNotifier(context)

    override suspend fun doWork(): Result {
        val uri = inputData.getString(LOCATION_URI_KEY)?.toUri()
        val options = inputData.getBooleanArray(OPTIONS_KEY)?.let { RestoreOptions.fromBooleanArray(it) }

        if (uri == null || options == null) {
            return Result.failure()
        }

        try {
            setForeground(getForegroundInfo())
        } catch (e: IllegalStateException) {
            Log.e("BackupRestoreWorker","Not allowed to run on foreground service")
        }

        return try {
            BackupRestorer(context, notifier).restore(uri, options)
            Result.success()
        } catch (e: Exception) {
            if (e is CancellationException) {
                notifier.showRestoreError(context.getString(R.string.restoring_backup_canceled))
                Result.success()
            } else {
                Log.e("BackupRestorer",e.stackTraceToString())
                notifier.showRestoreError(e.message)
                Result.failure()
            }
        } finally {
            context.cancelNotification(Notifications.RESTORE_PROGRESS_ID)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.RESTORE_PROGRESS_ID,
            notifier.showRestoreProgress().build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }


    companion object {
        fun isRunning(context: Context): Boolean {
            return context.workManager.isRunning(TAG)
        }

        fun start(
            context: Context,
            uri: Uri,
            options: RestoreOptions,
            sync: Boolean = false,
        ) {
            val inputData = workDataOf(
                LOCATION_URI_KEY to uri.toString(),
                OPTIONS_KEY to options.asBooleanArray(),
            )
            val request = OneTimeWorkRequestBuilder<BackupRestoreWorker>()
                .addTag(TAG)
                .setInputData(inputData)
                .build()
            context.workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
        }

        fun stop(context: Context) {
            context.workManager.cancelUniqueWork(TAG)
        }
    }
}

private const val TAG = "BackupRestore"

private const val LOCATION_URI_KEY = "location_uri" // String
private const val OPTIONS_KEY = "options" // BooleanArray
