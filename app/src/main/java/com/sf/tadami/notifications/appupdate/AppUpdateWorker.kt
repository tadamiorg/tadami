package com.sf.tadami.notifications.appupdate

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sf.tadami.R
import com.sf.tadami.network.GET
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.network.asObservable
import com.sf.tadami.network.newCachelessCallWithProgress
import com.sf.tadami.network.saveTo
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.notifications.utils.okhttp.ProgressListener
import com.sf.tadami.ui.utils.awaitSingleOrError
import com.sf.tadami.ui.utils.getUriCompat
import com.sf.tadami.utils.setForegroundSafely
import com.sf.tadami.utils.workManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.internal.http2.ErrorCode
import okhttp3.internal.http2.StreamResetException
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
        val url = inputData.getString(EXTRA_DOWNLOAD_URL)
        val title = inputData.getString(EXTRA_DOWNLOAD_TITLE) ?: context.getString(R.string.app_name)

        if (url.isNullOrEmpty()) {
            return Result.failure()
        }

        setForegroundSafely()

        withContext(Dispatchers.IO) {
            downloadApk(title, url)
        }

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.APP_UPDATE_DOWNLOAD_PROGRESS_ID,
            notifier.showProgressNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    private suspend fun downloadApk(title: String, url: String) {
        // Show notification download starting.
        notifier.showProgressNotification(title)

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

        try {
            // Download the new update.
            val response = network.client.newCachelessCallWithProgress(GET(url), progressListener)
                .asObservable().awaitSingleOrError()

            // File where the apk will be saved.
            val apkFile = File(context.externalCacheDir, "update.apk")

            if (response.isSuccessful) {
                response.body.source().saveTo(apkFile)
            } else {
                response.close()
                throw Exception("Unsuccessful response")
            }
            notifier.cancel()
            notifier.promptInstall(apkFile.getUriCompat(context))
        } catch (e: Exception) {
            val shouldCancel = e is CancellationException ||
                    (e is StreamResetException && e.errorCode == ErrorCode.CANCEL)
            if (shouldCancel) {
                notifier.cancel()
            } else {
                notifier.onDownloadError(url)
            }
        }
    }


    companion object {
        private const val TAG = "AppUpdate"
        const val EXTRA_DOWNLOAD_URL = "DOWNLOAD_URL"
        const val EXTRA_DOWNLOAD_TITLE = "DOWNLOAD_TITLE"

        fun start(
            context: Context,
            url: String,
            title: String? = null
        ): Boolean {
            val constraints = Constraints(
                requiredNetworkType = NetworkType.CONNECTED,
            )
            val request = OneTimeWorkRequestBuilder<AppUpdateWorker>()
                .setConstraints(constraints)
                .addTag(TAG)
                .setInputData(
                    workDataOf(
                        EXTRA_DOWNLOAD_URL to url,
                        EXTRA_DOWNLOAD_TITLE to title,
                    ),
                )
                .build()

            context.workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)

            return true
        }

        fun stop(context: Context) {
            context.workManager.cancelUniqueWork(TAG)
        }
    }
}