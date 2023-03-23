package com.sf.tadami.notifications.libraryupdate

import android.content.Context
import android.util.Log
import androidx.work.*
import com.sf.tadami.data.interactors.AnimeWithEpisodesInteractor
import com.sf.tadami.data.interactors.FavoriteInteractor
import com.sf.tadami.data.interactors.UpdateAnimeInteractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.FavoriteAnime
import com.sf.tadami.domain.anime.toAnime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.ui.tabs.animesources.AnimeSourcesManager
import com.sf.tadami.ui.utils.awaitSingleOrError
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class LibraryUpdateWorker(
    private val context : Context,
    params : WorkerParameters
) : CoroutineWorker(context,params) {

    private val notifier = LibraryUpdateNotifier(context)
    private val favoriteInteractor : FavoriteInteractor = Injekt.get()
    private val sourcesManager : AnimeSourcesManager = Injekt.get()
    private val animeWithEpisodesInteractor : AnimeWithEpisodesInteractor = Injekt.get()
    private val updateAnimeInteractor : UpdateAnimeInteractor = Injekt.get()

    private var animesToUpdate: List<FavoriteAnime> = mutableListOf()

    override suspend fun doWork(): Result {
        try {
            setForeground(getForegroundInfo())
        } catch (e: IllegalStateException) {
            Log.d("Worker error", "Not allowed to set foreground job", e)
        }

        addAnimesToQueue()

        return withContext(Dispatchers.IO) {
            try {
                updateChapterList()
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

    private fun addAnimesToQueue() {
        val favoriteAnimes = runBlocking { favoriteInteractor.await() }

        animesToUpdate = favoriteAnimes.sortedBy { it.title }
    }


    private suspend fun updateChapterList() {
        val semaphore = Semaphore(5)
        val progressCount = AtomicInteger(0)
        val newUpdates = CopyOnWriteArrayList<Pair<Anime, Array<Episode>>>()
        val skippedUpdates = CopyOnWriteArrayList<Pair<Anime, String?>>()
        val failedUpdates = CopyOnWriteArrayList<Pair<Episode, String?>>()
        coroutineScope {
            animesToUpdate.groupBy { it.source }.values
                .map { animeInSource ->
                    async {
                        semaphore.withPermit {
                            animeInSource.forEach { favoriteAnime ->
                                val anime = favoriteAnime.toAnime()
                                ensureActive()

                                if (!animeWithEpisodesInteractor.awaitAnime(anime.id).favorite) {
                                    return@forEach
                                }

                                withUpdateNotification(
                                    progressCount,
                                ) {
                                    try {
                                        val newEpisodes = updateAnime(anime)
                                        if (newEpisodes.isNotEmpty()) {
                                            newUpdates.add(anime to newEpisodes.toTypedArray())
                                        }
                                    } catch (e: Throwable) {
                                        Log.e("Update failed", e.message.toString())
                                    }
                                }
                            }
                        }
                    }
                }
                .awaitAll()
        }

        notifier.cancelProgressNotification()
        Log.e("Updates success",newUpdates.toString())
        if (newUpdates.isNotEmpty()) {
            notifier.showUpdateNotifications(newUpdates)
        }

        /*if (failedUpdates.isNotEmpty()) {
            val errorFile = writeErrorFile(failedUpdates)
            notifier.showFailureNotifications(
                failedUpdates.size,
                errorFile.getUriCompat(context),
            )
        }*/

    }
    private suspend fun withUpdateNotification(
        completed: AtomicInteger,
        block: suspend () -> Unit,
    ) {
        coroutineScope {
            ensureActive()

            notifier.showProgressNotification(
                completed.get(),
                animesToUpdate.size,
            )

            block()

            ensureActive()

            completed.getAndIncrement()
            notifier.showProgressNotification(
                completed.get(),
                animesToUpdate.size,
            )
        }
    }

    private suspend fun updateAnime(anime: Anime) : List<Episode> {
        val source = sourcesManager.getExtensionById(anime.source) ?: return emptyList()
        val episodes = source.fetchEpisodesList(anime).awaitSingleOrError() ?: return emptyList()
        val dbAnime = animeWithEpisodesInteractor.awaitAnime(anime.id).takeIf { it.favorite } ?: return emptyList()

        return updateAnimeInteractor.awaitEpisodesSyncFromSource(dbAnime,episodes)
    }

    companion object{
        private const val TAG = "LibraryUpdate"
        private const val WORK_NAME = "library_update_work"

        fun startNow(
            context: Context,
        ): Boolean {
            val wm = WorkManager.getInstance(context)
            val infos = wm.getWorkInfosByTag(TAG).get()
            if (infos.find { it.state == WorkInfo.State.RUNNING } != null) {
                return false
            }

            val request = OneTimeWorkRequestBuilder<LibraryUpdateWorker>()
                .addTag(TAG)
                .addTag(WORK_NAME)
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