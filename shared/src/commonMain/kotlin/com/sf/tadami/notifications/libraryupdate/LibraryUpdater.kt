package com.sf.tadami.notifications.libraryupdate

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.sf.tadami.data.interactors.anime.AnimeWithEpisodesInteractor
import com.sf.tadami.data.interactors.anime.FetchIntervalInteractor
import com.sf.tadami.data.interactors.anime.UpdateAnimeInteractor
import com.sf.tadami.data.interactors.library.LibraryInteractor
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.anime.LibraryAnime
import com.sf.tadami.domain.anime.toAnime
import com.sf.tadami.domain.anime.toSAnime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.extension.util.callGuarded
import com.sf.tadami.preferences.library.LibraryPreferences
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.notification_library_update_skipped_not_caught_up
import com.sf.tadami.shared.resources.notification_refreshing_library_details
import com.sf.tadami.shared.resources.notification_updating_library
import com.sf.tadami.source.StubSource
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.utils.editPreferences
import com.sf.tadami.utils.stringRes
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.compose.resources.StringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.time.ZonedDateTime
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/** What a library update reports as it goes: the notifications on Android. */
interface LibraryUpdateListener {

    /** [done] of the [total] anime updated, under [title]. */
    fun onProgress(done: Int, total: Int, title: StringResource) = Unit

    /** The anime updated, no more progress to show. */
    fun onProgressEnd() = Unit

    /** [count] anime not caught up were skipped, listed in [file]. */
    fun onSkipped(count: Int, file: File) = Unit

    /** The anime that got new episodes. */
    fun onUpdates(updates: List<Pair<Anime, Array<Episode>>>) = Unit

    /** [count] anime failed to update, the errors in [file]. */
    fun onFailures(count: Int, file: File) = Unit
}

/**
 * Updates the anime of the library, or only those of [categoryId], fetching [target]: the work of
 * LibraryUpdateWorker on Android. The lists of the skipped and failed anime go to [cacheDir].
 */
class LibraryUpdater(
    private val target: LibraryUpdateTarget,
    private val categoryId: Long?,
    private val cacheDir: File,
    private val listener: LibraryUpdateListener,
) {
    private val libraryInteractor: LibraryInteractor = Injekt.get()
    private val sourcesManager: SourceManager = Injekt.get()
    private val animeWithEpisodesInteractor: AnimeWithEpisodesInteractor = Injekt.get()
    private val updateAnimeInteractor: UpdateAnimeInteractor = Injekt.get()
    private val dataStore: DataStore<Preferences> = Injekt.get()
    private val fetchIntervalInteractor: FetchIntervalInteractor = Injekt.get()
    private var animesToUpdate: List<LibraryAnime> = mutableListOf()
    private val progressTitle = when (target) {
        LibraryUpdateTarget.EPISODES -> Res.string.notification_updating_library
        LibraryUpdateTarget.DETAILS -> Res.string.notification_refreshing_library_details
    }

    suspend fun run(libraryPreferences: LibraryPreferences) {
        // A refresh of the details isn't a library update
        if (target == LibraryUpdateTarget.EPISODES) {
            dataStore.editPreferences(
                libraryPreferences.copy(lastUpdatedTimestamp = Date().time),
                LibraryPreferences
            )
        }
        addAnimesToQueue(libraryPreferences)
        updateEpisodeList(libraryPreferences)
    }

    private suspend fun addAnimesToQueue(libraryPreferences: LibraryPreferences) {
        val libraryAnimes = libraryInteractor.await()
        // The details of every anime, without the update settings nor the not caught up ones skipped, as Mihon
        if (target == LibraryUpdateTarget.DETAILS) {
            animesToUpdate = libraryAnimes.toRefresh(categoryId).sortedBy { it.title }
            return
        }
        val listToUpdate = libraryAnimes
            .toUpdate(categoryId, libraryPreferences.updateCategories, libraryPreferences.updateCategoriesExclude)
            .sortedBy { it.title }

        val skippedUpdates = mutableListOf<Pair<Anime, String?>>()
        animesToUpdate = listToUpdate.filter {
            if (it.unseenEpisodes <= 5L) return@filter true
            skippedUpdates.add(it.toAnime() to stringRes(Res.string.notification_library_update_skipped_not_caught_up))
            false
        }

        if (skippedUpdates.isNotEmpty()) {
            val skippedFile = writeListFile("tadami_update_skipped.txt", skippedUpdates)
            listener.onSkipped(skippedUpdates.size, skippedFile)
        }
    }

    private suspend fun updateEpisodeList(libraryPreferences: LibraryPreferences) {
        val parts = target.parts(libraryPreferences.autoUpdateMetadata)
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
                                    withProgress(progressCount) {
                                        try {
                                            val newEpisodes =
                                                updateAnime(anime, fetchWindow, parts, libraryPreferences)
                                                    .sortedBy { it.sourceOrder }
                                            if (newEpisodes.isNotEmpty()) {
                                                // Only this key, read and written in one edit: a read then a write of the
                                                // whole group loses parallel increments and reverts sort or filter changes
                                                dataStore.edit {
                                                    val count = it[LibraryPreferences.NEW_UPDATES_COUNT] ?: 0
                                                    it[LibraryPreferences.NEW_UPDATES_COUNT] = count + newEpisodes.size
                                                }
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
        listener.onProgressEnd()

        if (newUpdates.isNotEmpty()) {
            listener.onUpdates(newUpdates)
        }

        if (failedUpdates.isNotEmpty()) {
            val errorFile = writeListFile("tadami_update_errors.txt", failedUpdates)
            listener.onFailures(failedUpdates.size, errorFile)
        }
    }

    private suspend fun withProgress(completed: AtomicInteger, block: suspend () -> Unit) {
        coroutineScope {
            ensureActive()
            if (completed.get() == 0) {
                listener.onProgress(completed.get(), animesToUpdate.size, progressTitle)
            }
            block()
            ensureActive()
            completed.getAndIncrement()
            listener.onProgress(completed.get(), animesToUpdate.size, progressTitle)
        }
    }

    private suspend fun updateAnime(
        anime: Anime,
        fetchWindow: Pair<Long, Long>,
        parts: LibraryUpdateParts,
        libraryPreferences: LibraryPreferences,
    ): List<Episode> {
        val source = sourcesManager.getOrStub(anime.source)
        if (source is StubSource) return emptyList()
        if (parts.details) {
            val dbAnime = animeWithEpisodesInteractor.awaitAnime(anime.id).takeIf { it.favorite }
                ?: return emptyList()
            val details = source.callGuarded { getAnimeDetails(dbAnime.toSAnime()) }
            // A refreshed cover gets a new image cache key, so the screens showing it load it again
            updateAnimeInteractor.awaitUpdateFromSource(
                dbAnime,
                details,
                updateLibraryTitles = libraryPreferences.updateTitles,
                refreshCover = parts.refreshCovers,
            )
        }
        if (!parts.episodes) return emptyList()
        val episodes = source.callGuarded { getEpisodeList(anime.toSAnime()) }
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

    /** The anime of [entries] by reason then source, in the cache file [name]. */
    private fun writeListFile(name: String, entries: List<Pair<Anime, String?>>): File {
        try {
            if (entries.isNotEmpty()) {
                val file = File(cacheDir, name)
                if (file.exists()) {
                    file.delete()
                }
                file.createNewFile()
                file.bufferedWriter().use { out ->
                    entries.groupBy({ it.second }, { it.first }).forEach { (reason, animes) ->
                        out.write("\n! ${reason}\n")
                        animes.groupBy { it.source }.forEach { (srcId, animes) ->
                            val sourceName = sourcesManager.getOrStub(srcId).name.takeIf { it.isNotEmpty() }
                            out.write("  # ${sourceName ?: srcId}\n")
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
}
