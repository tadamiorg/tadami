package com.sf.tadami.notifications.appupdate

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import com.sf.tadami.network.GET
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.asObservableSuccess
import com.sf.tadami.network.newCachelessCallWithProgress
import com.sf.tadami.network.saveTo
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.notifications.utils.okhttp.ProgressListener
import com.sf.tadami.ui.utils.awaitSingleOrNull
import com.sf.tadami.ui.utils.getUriCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File


class AppUpdateWorker(
    val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notifier = AppUpdateNotifier(context)
    private val network: NetworkHelper = Injekt.get()

    override suspend fun doWork(): Result {
        try {
            setForegroundAsync(getForegroundInfo())
        } catch (e: IllegalStateException) {
            Log.d("Worker error", "Job could not be set in foreground", e)
        }
        return withContext(Dispatchers.IO) {
            try {
                downloadAppUpdate(inputData.getString(UPDATE_LINK)!!)
                Result.success()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Assume success although cancelled
                    Result.success()
                } else {
                    Log.d("Worker error performing task", "error", e)
                    Result.failure()
                }
            } finally {
                notifier.cancelProgressNotification()
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.APP_UPDATE_DOWNLOAD_PROGRESS_ID,
            notifier.showProgressNotification()
        )
    }

    private suspend fun downloadAppUpdate(updateLink : String) {
        val progressListener = object : ProgressListener {
            // Progress of the download
            var savedProgress = 0

            // Keep track of the last notification sent to avoid posting too many.
            var lastTick = 0L

            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                val progress = (100 * (bytesRead.toFloat() / contentLength)).toInt()
                val currentTime = System.currentTimeMillis()
                if (progress > savedProgress && currentTime - 200 > lastTick) {
                    savedProgress = progress
                    lastTick = currentTime
                    notifier.onProgressChange(progress)
                }
            }
        }

        // Download the new update.
        val call: Call = network.client.newCachelessCallWithProgress(GET(updateLink),progressListener)
        val response = call.asObservableSuccess().awaitSingleOrNull() ?: throw Exception("Error while making the call to download the apk")

        // File where the apk will be saved.
        val apkFile = File(context.externalCacheDir, "update.apk")

        if (response.isSuccessful) {
            response.body.source().saveTo(apkFile)
        } else {
            response.close()
            throw Exception("Unsuccessful response")
        }
        notifier.showInstallNotification(apkFile.getUriCompat(context))
    }

    companion object {
        private const val TAG = "AppUpdate"
        private const val WORK_NAME = "app_update_work"
        private const val UPDATE_LINK = "github_update_link"

        fun startNow(
            context: Context,
            updateLink : String
        ): Boolean {
            val wm = WorkManager.getInstance(context)
            val infos = wm.getWorkInfosByTag(TAG).get()
            if (infos.find { it.state == WorkInfo.State.RUNNING } != null) {
                return false
            }

            val data = Data.Builder()
            data.putString(UPDATE_LINK,updateLink)

            val request = OneTimeWorkRequestBuilder<AppUpdateWorker>()
                .addTag(TAG)
                .addTag(WORK_NAME)
                .setInputData(data.build())
                .build()
            wm.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)

            return true
        }

        fun stop(context: Context) {
            val wm = WorkManager.getInstance(context)
            val workQuery = WorkQuery.Builder.fromTags(listOf(TAG))
                .addStates(listOf(WorkInfo.State.RUNNING))
                .build()
            wm.getWorkInfos(workQuery).get()
                // Should only return one work but just in case
                .forEach {
                    wm.cancelWorkById(it.id)
                }
        }
    }
}