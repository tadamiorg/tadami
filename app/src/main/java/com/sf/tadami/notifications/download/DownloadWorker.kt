package com.sf.tadami.notifications.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import com.sf.tadami.data.download.TadamiDownloadManager
import com.sf.tadami.data.download.model.Download
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.utils.logcat
import com.sf.tadami.utils.workManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Downloads the queue in the background, as a foreground service of type dataSync so it goes on
 * once the app is left. WorkManager brings the network constraint (Wi-Fi only or not), and runs
 * the work again after a process death or when the network comes back. Stopping the work pauses
 * the downloads: they resume where they were.
 */
class DownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val downloadManager: TadamiDownloadManager = Injekt.get()
    private val notifier = DownloadNotifier(context)

    override suspend fun doWork(): Result {
        try {
            setForeground(getForegroundInfo())
        } catch (e: IllegalStateException) {
            // Not allowed from the background on Android 12+: runs as a plain job, stopped after
            // 10 minutes, then run again later
            logcat(LogPriority.WARN, e) { "Downloads not run in a foreground service" }
        }
        return try {
            coroutineScope {
                val notifications = launch {
                    while (isActive) {
                        val queue = downloadManager.queueState.value
                        notifier.showProgress(
                            active = queue.filter { it.status == Download.State.DOWNLOADING },
                            queued = queue.count { it.status == Download.State.QUEUED || it.status == Download.State.DOWNLOADING },
                        )
                        delay(NOTIFICATION_INTERVAL_MS)
                    }
                }
                val errors = launch {
                    downloadManager.statusFlow()
                        .filter { it.status == Download.State.ERROR }
                        .collect { notifier.showError(it) }
                }
                downloadManager.runQueue()
                notifications.cancel()
                errors.cancel()
            }
            Result.success()
        } finally {
            notifier.dismissProgress()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.DOWNLOADER_PROGRESS_ID,
            notifier.progressNotificationBuilder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    companion object {
        private const val WORK_NAME = "episode_downloads"
        private const val NOTIFICATION_INTERVAL_MS = 1000L

        /** Starts the queue, replacing a run waiting for its network or ending. */
        fun start(context: Context, onlyOverWifi: Boolean) {
            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .addTag(WORK_NAME)
                .setConstraints(
                    Constraints(
                        requiredNetworkType = if (onlyOverWifi) NetworkType.UNMETERED else NetworkType.CONNECTED,
                    ),
                )
                .build()
            context.workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun stop(context: Context) {
            context.workManager.cancelUniqueWork(WORK_NAME)
        }

        /** Whether the work runs or waits for its network, each time it changes. */
        fun isScheduledFlow(context: Context): Flow<Boolean> {
            return context.workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME)
                .map { infos -> infos.any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING } }
                .distinctUntilChanged()
        }

        /** Running, or waiting for its network. Blocks: not on the main thread. */
        fun isScheduled(context: Context): Boolean {
            return context.workManager.getWorkInfosForUniqueWork(WORK_NAME).get()
                .any { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING }
        }
    }
}
