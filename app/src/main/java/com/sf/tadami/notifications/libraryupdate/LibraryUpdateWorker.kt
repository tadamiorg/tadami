package com.sf.tadami.notifications.libraryupdate

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import com.sf.tadami.utils.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.ui.utils.getUriCompat
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.isConnectedToWifi
import com.sf.tadami.utils.isRunning
import com.sf.tadami.utils.workManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.util.concurrent.TimeUnit

class LibraryUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notifier = LibraryUpdateNotifier(context)
    private val dataStore: DataStore<Preferences> = Injekt.get()
    private val libraryPreferences = runBlocking {
        dataStore.getPreferencesGroup(LibraryPreferences)
    }
    private val target = LibraryUpdateTarget.of(params.inputData.getString(KEY_TARGET))

    override suspend fun doWork(): Result {
        if (tags.contains(WORK_NAME_AUTO)) {
            val restrictions = libraryPreferences.autoUpdateRestrictions
            if ((LibraryPreferences.AutoUpdateRestrictionItems.WIFI in restrictions) && !context.isConnectedToWifi()) {
                return Result.retry()
            }

            // Find a running manual worker. If exists, try again later
            if (context.workManager.isRunning(WORK_NAME_MANUAL)) {
                return Result.retry()
            }
        }

        try {
            setForeground(getForegroundInfo())
        } catch (e: IllegalStateException) {
            Log.d("Worker error", "Job could not be set in foreground", e)
        }

        val updater = LibraryUpdater(
            target = target,
            categoryId = inputData.getLong(KEY_CATEGORY, -1L).takeIf { it >= 0L },
            cacheDir = context.externalCacheDir ?: context.cacheDir,
            listener = object : LibraryUpdateListener {
                override fun onProgress(done: Int, total: Int, title: StringResource) =
                    notifier.showProgressNotification(done, total, title)

                override fun onProgressEnd() = notifier.cancelProgressNotification()

                override fun onSkipped(count: Int, file: File) =
                    notifier.showSkippedNotifications(count, file.getUriCompat(context))

                override fun onUpdates(updates: List<Pair<Anime, Array<Episode>>>) = notifier.showUpdateNotifications(updates)

                override fun onFailures(count: Int, file: File) =
                    notifier.showFailureNotifications(count, file.getUriCompat(context))
            },
        )

        return withContext(Dispatchers.IO) {
            try {
                updater.run(libraryPreferences)
                Result.success()
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Assume success although cancelled
                    Result.success()
                } else {
                    Log.d("Worker error", "error", e)
                    Result.failure()
                }
            } finally {
                notifier.cancelProgressNotification()
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            Notifications.LIBRARY_UPDATE_PROGRESS_ID,
            notifier.progressNotificationBuilder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    companion object {
        private const val TAG = "LibraryUpdate"
        private const val WORK_NAME_MANUAL = "library_update_work_manual"
        private const val WORK_NAME_AUTO = "library_update_work_auto"
        private const val KEY_CATEGORY = "category"
        private const val KEY_TARGET = "target"

        fun setupTask(
            context: Context,
            prefInterval: Int? = null,
        ) {
            val dataStore: DataStore<Preferences> = Injekt.get()
            val libraryPreferences = runBlocking {
                dataStore.getPreferencesGroup(LibraryPreferences)
            }
            val interval = prefInterval ?: libraryPreferences.autoUpdateInterval
            if (interval > 0) {
                val restrictions = libraryPreferences.autoUpdateRestrictions
                val constraints = Constraints(
                    requiredNetworkType = NetworkType.CONNECTED,
                    requiresCharging = LibraryPreferences.AutoUpdateRestrictionItems.CHARGE in restrictions,
                    requiresBatteryNotLow = LibraryPreferences.AutoUpdateRestrictionItems.BATTERY in restrictions,
                )

                val request = PeriodicWorkRequestBuilder<LibraryUpdateWorker>(
                    interval.toLong(),
                    TimeUnit.HOURS,
                    10,
                    TimeUnit.MINUTES
                )
                    .addTag(TAG)
                    .addTag(WORK_NAME_AUTO)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                    .build()

                context.workManager.enqueueUniquePeriodicWork(
                    WORK_NAME_AUTO,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            } else {
                context.workManager.cancelUniqueWork(WORK_NAME_AUTO)
            }
        }

        /** Updates the library, or only the anime of [categoryId], fetching [target] */
        fun startNow(
            context: Context,
            categoryId: Long? = null,
            target: LibraryUpdateTarget = LibraryUpdateTarget.EPISODES,
        ): Boolean {
            val wm = context.workManager
            if (wm.isRunning(TAG)) {
                return false
            }

            val request = OneTimeWorkRequestBuilder<LibraryUpdateWorker>()
                .addTag(TAG)
                .addTag(WORK_NAME_MANUAL)
                .setInputData(workDataOf(KEY_CATEGORY to (categoryId ?: -1L), KEY_TARGET to target.name))
                .build()
            wm.enqueueUniqueWork(WORK_NAME_MANUAL, ExistingWorkPolicy.KEEP, request)

            return true
        }

        fun stop(context: Context) {
            val wm = context.workManager
            val workQuery = WorkQuery.Builder.fromTags(listOf(TAG))
                .addStates(listOf(WorkInfo.State.RUNNING))
                .build()
            wm.getWorkInfos(workQuery).get()
                // Should only return one work but just in case
                .forEach {
                    wm.cancelWorkById(it.id)

                    // Re-enqueue cancelled scheduled work
                    if (it.tags.contains(WORK_NAME_AUTO)) {
                        setupTask(context)
                    }
                }
        }

    }
}