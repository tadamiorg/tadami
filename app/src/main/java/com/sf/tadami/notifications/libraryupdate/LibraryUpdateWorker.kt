package com.sf.tadami.notifications.libraryupdate

import android.content.Context
import android.util.Log
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
import com.sf.tadami.R
import com.sf.tadami.data.interactors.anime.AnimeWithEpisodesInteractor
import com.sf.tadami.data.interactors.anime.FetchIntervalInteractor
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.data.interactors.library.LibraryInteractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.domain.anime.toAnime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.notifications.Notifications
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.source.online.StubSource
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.utils.awaitSingleOrError
import com.sf.tadami.ui.utils.getUriCompat
import com.sf.tadami.utils.createFileInCacheDir
import com.sf.tadami.utils.editPreferences
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.isConnectedToWifi
import com.sf.tadami.utils.isRunning
import com.sf.tadami.utils.workManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.time.ZonedDateTime
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class LibraryUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notifier = LibraryUpdateNotifier(context)
    private val libraryInteractor: LibraryInteractor = Injekt.get()
    private val sourcesManager: SourceManager = Injekt.get()
    private val animeWithEpisodesInteractor: AnimeWithEpisodesInteractor = Injekt.get()
    private val updateAnimeInteractor: UpdateAnimeInteractor = Injekt.get()
    private val dataStore: DataStore<Preferences> = Injekt.get()
    private var animesToUpdate: List<LibraryAnime> = mutableListOf()
    private val fetchIntervalInteractor: FetchIntervalInteractor = Injekt.get()
    private val libraryPreferences = runBlocking {
        dataStore.getPreferencesGroup(LibraryPreferences)
    }

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

        dataStore.editPreferences(
            libraryPreferences.copy(lastUpdatedTimestamp = Date().time),
            LibraryPreferences
        )

        addAnimesToQueue()

        return withContext(Dispatchers.IO) {
            try {
                updateEpisodeList()
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
            notifier.progressNotificationBuilder.build()
        )
    }

    private fun addAnimesToQueue() {
        val libraryAnimes = runBlocking { libraryInteractor.await() }
        val listToUpdate = libraryAnimes.sortedBy { it.title }

        val skippedUpdates = mutableListOf<Pair<Anime, String?>>()
        animesToUpdate = listToUpdate.filter {
            if (it.unseenEpisodes <= 5L) return@filter true
            skippedUpdates.add(it.toAnime() to context.getString(R.string.notification_library_update_skipped_not_caught_up))
            false
        }

        if (skippedUpdates.isNotEmpty()) {
            val skippedFile = writeSkippedFile(skippedUpdates)
            notifier.showSkippedNotifications(
                skippedUpdates.size,
                skippedFile.getUriCompat(context)
            )
        }

    }


    private suspend fun updateEpisodeList() {
        val semaphore = Semaphore(5)
        val progressCount = AtomicInteger(0)
        val newUpdates = CopyOnWriteArrayList<Pair<Anime, Array<Episode>>>()
        val failedUpdates = CopyOnWriteArrayList<Pair<Anime, String?>>()
        val fetchWindow = fetchIntervalInteractor.getWindow(ZonedDateTime.now())
        coroutineScope {
            animesToUpdate.groupBy { it.source }.values
                .map { animeInSource ->
                    async {
                        semaphore.withPermit {
                            animeInSource.forEach { libraryAnime ->
                                val anime = libraryAnime.toAnime()
                                ensureActive()

                                if (animeWithEpisodesInteractor.awaitAnime(anime.id).favorite) {
                                    withUpdateNotification(
                                        progressCount
                                    ) {

                                        try {
                                            val newEpisodes =
                                                updateAnime(
                                                    anime,
                                                    fetchWindow
                                                ).sortedBy { it.sourceOrder }
                                            if (newEpisodes.isNotEmpty()) {
                                                val newUpdatesCount = dataStore.getPreferencesGroup(
                                                    LibraryPreferences
                                                ).let {
                                                    it.copy(newUpdatesCount = it.newUpdatesCount + newEpisodes.size)
                                                }
                                                dataStore.editPreferences(
                                                    newValue = newUpdatesCount,
                                                    preferences = LibraryPreferences
                                                )
                                                newUpdates.add(anime to newEpisodes.toTypedArray())
                                            }
                                        } catch (e: Throwable) {
                                            failedUpdates.add(anime to e.message)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .awaitAll()
        }
        notifier.cancelProgressNotification()

        if (newUpdates.isNotEmpty()) {
            notifier.showUpdateNotifications(newUpdates)
        }

        if (failedUpdates.isNotEmpty()) {
            val errorFile = writeErrorFile(failedUpdates)
            notifier.showFailureNotifications(
                failedUpdates.size,
                errorFile.getUriCompat(context),
            )
        }
    }

    private suspend fun withUpdateNotification(
        completed: AtomicInteger,
        block: suspend () -> Unit,

        ) {
        coroutineScope {
            ensureActive()
            if (completed.get() == 0) {
                notifier.showProgressNotification(
                    completed.get(),
                    animesToUpdate.size,
                )
            }
            block()
            ensureActive()
            completed.getAndIncrement()
            notifier.showProgressNotification(
                completed.get(),
                animesToUpdate.size,
            )
        }
    }

    private suspend fun updateAnime(anime: Anime, fetchWindow: Pair<Long, Long>): List<Episode> {
        val source = sourcesManager.getOrStub(anime.source)
        if (source is StubSource) return emptyList()
        val episodes = source.fetchEpisodesList(anime).awaitSingleOrError()
        val dbAnime = animeWithEpisodesInteractor.awaitAnime(anime.id).takeIf { it.favorite }
            ?: return emptyList()

        return updateAnimeInteractor.awaitEpisodesSyncFromSource(
            dbAnime,
            episodes,
            source,
            false,
            fetchWindow
        )
    }

    private fun writeErrorFile(errors: List<Pair<Anime, String?>>): File {
        try {
            if (errors.isNotEmpty()) {
                val file = context.createFileInCacheDir("tadami_update_errors.txt")
                file.bufferedWriter().use { out ->
                    errors.groupBy({ it.second }, { it.first }).forEach { (error, animes) ->
                        out.write("\n! ${error}\n")
                        animes.groupBy { it.source }.forEach { (srcId, animes) ->
                            out.write("  # $srcId\n")
                            animes.forEach {
                                out.write("    - ${it.title}\n")
                            }
                        }
                    }
                }
                return file
            }
        } catch (_: Exception) {
        }
        return File("")
    }

    private fun writeSkippedFile(skipped: List<Pair<Anime, String?>>): File {
        try {
            if (skipped.isNotEmpty()) {
                val file = context.createFileInCacheDir("tadami_update_skipped.txt")
                file.bufferedWriter().use { out ->
                    skipped.groupBy({ it.second }, { it.first }).forEach { (skipReason, animes) ->
                        out.write("\n! ${skipReason}\n")
                        animes.groupBy { it.source }.forEach { (srcId, animes) ->
                            out.write("  # $srcId\n")
                            animes.forEach {
                                out.write("    - ${it.title}\n")
                            }
                        }
                    }
                }
                return file
            }
        } catch (_: Exception) {
        }
        return File("")
    }

    companion object {
        private const val TAG = "LibraryUpdate"
        private const val WORK_NAME_MANUAL = "library_update_work_manual"
        private const val WORK_NAME_AUTO = "library_update_work_auto"

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

        fun startNow(
            context: Context,
        ): Boolean {
            val wm = context.workManager
            if (wm.isRunning(TAG)) {
                return false
            }

            val request = OneTimeWorkRequestBuilder<LibraryUpdateWorker>()
                .addTag(TAG)
                .addTag(WORK_NAME_MANUAL)
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