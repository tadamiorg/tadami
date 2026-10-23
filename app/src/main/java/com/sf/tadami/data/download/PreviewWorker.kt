package com.sf.tadami.data.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.sf.tadami.utils.workManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Makes the seekbar preview frames of the downloaded episodes waiting ([PreviewGenerator.makeAll]), as a plain
 * background job: while it runs, the process is neither frozen nor killed, with the app and the downloads' service
 * gone, and WorkManager runs it again after a stop or a process death (the episodes waiting are saved). Neither a
 * notification nor a foreground service: the frames can wait, unlike a download, and an episode's take a minute or
 * two, within a job's time. It ends while the player screen or a cast is active, and runs again after them.
 */
class PreviewWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) { Injekt.get<TadamiDownloadManager>().makePreviews() }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "episode_previews"

        /** Runs it soon, after the run in progress if any: that one may be ending, having seen nothing more to make. */
        fun start(context: Context) {
            context.workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<PreviewWorker>().build(),
            )
        }
    }
}
