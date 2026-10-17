package com.sf.tadami.data.download

import android.app.Application
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.download.model.Download
import com.sf.tadami.data.download.model.EpisodeDownloadState
import com.sf.tadami.domain.anime.Anime
import com.sf.tadami.domain.episode.Episode
import com.sf.tadami.notifications.download.DownloadNotifier
import com.sf.tadami.notifications.download.DownloadWorker
import com.sf.tadami.preferences.download.DownloadPreferences
import com.sf.tadami.source.Source
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.logcat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File

/**
 * Entry point of the episode downloads: the queue and its controls, the deletions, and the files
 * of the downloaded episodes. See [Downloader] for the engine and [DownloadWorker] for how it runs
 * in the background.
 */
class TadamiDownloadManager(
    private val context: Application = Injekt.get(),
    private val provider: DownloadProvider = Injekt.get(),
) {
    private val sourceManager: SourceManager by injectLazy()
    private val animeRepository: AnimeRepository by injectLazy()
    private val dataStore: DataStore<Preferences> by injectLazy()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val notifier by lazy { DownloadNotifier(context) }

    private val cache = DownloadCache(provider, Injekt.get(), scope)

    private val downloader = File(context.filesDir, DOWNLOADS_DATA_DIR).let { dir ->
        Downloader(
            context = context,
            provider = provider,
            cache = cache,
            store = DownloadStore(File(dir, "queue.json")),
            resumeStore = ResumeStore(File(dir, "resume")),
            scope = scope,
        )
    }

    /** The queue, in download order. Downloaded episodes leave it, failed ones stay. */
    val queueState: StateFlow<List<Download>>
        get() = downloader.queueState

    /** Whether the queue is being downloaded. A queue not empty and not running is paused. */
    val isRunning: StateFlow<Boolean>
        get() = downloader.isRunning

    /** Whether the queue runs or waits for its network: false once paused. */
    fun isScheduledFlow(): Flow<Boolean> = DownloadWorker.isScheduledFlow(context)

    /** Changes each time the set of downloaded episodes does */
    val cacheChanges: StateFlow<Long>
        get() = cache.changes

    /** Each queued download whose status changes. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun statusFlow(): Flow<Download> = queueState.flatMapLatest { downloads ->
        downloads.map { download -> download.statusFlow.drop(1).map { download } }.merge()
    }

    /** Each queued download whose progress changes, by percent. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun progressFlow(): Flow<Download> = queueState.flatMapLatest { downloads ->
        downloads.map { download -> download.progressFlow.drop(1).map { download } }.merge()
    }

    /** Queues [episodes] of [anime], the downloaded and queued ones left out, and starts. */
    fun downloadEpisodes(anime: Anime, episodes: List<Episode>, autoStart: Boolean = true) {
        if (episodes.isEmpty()) return
        scope.launch {
            val source = sourceManager.getOrStub(anime.source)
            val downloaded = DownloadNames.EpisodeDirs(
                cache.episodeDirs(provider.getSourceDirName(source), provider.getAnimeDirName(anime)),
            )
            val downloads = episodes
                .filter { downloaded.find(it) == null }
                .map { Download(source, anime, it, DownloadNames.episodeDirName(it)) }
            downloader.add(downloads)
            if (autoStart) startDownloads()
        }
    }

    /** Starts or resumes the queue. */
    fun startDownloads() {
        notifier.dismissPaused()
        scope.launch {
            downloader.awaitRestored()
            if (queueState.value.none { it.status == Download.State.QUEUED }) return@launch
            if (downloader.signalIfRunning()) return@launch
            DownloadWorker.start(context, dataStore.getPreferencesGroup(DownloadPreferences).onlyOverWifi)
        }
    }

    /** Stops the downloads where they are: they stay queued and resume from there. */
    fun pauseDownloads() {
        DownloadWorker.stop(context)
        notifier.dismissProgress()
        val queued = queueState.value.count { it.status == Download.State.QUEUED || it.status == Download.State.DOWNLOADING }
        if (queued > 0) notifier.showPaused(queued)
    }

    /** Applies changed settings (the Wi-Fi only constraint) to a queue running or waiting for its network. */
    fun restartIfRunning() {
        scope.launch {
            if (!DownloadWorker.isScheduled(context)) return@launch
            DownloadWorker.start(context, dataStore.getPreferencesGroup(DownloadPreferences).onlyOverWifi)
        }
    }

    /** Empties the queue, deleting what was partly downloaded. */
    fun clearQueue() {
        DownloadWorker.stop(context)
        downloader.remove(queueState.value)
        notifier.dismissProgress()
        notifier.dismissPaused()
    }

    fun cancelQueuedDownloads(downloads: List<Download>) {
        downloader.remove(downloads)
    }

    /** [downloads] is the new order of the queue. */
    fun reorderQueue(downloads: List<Download>) {
        downloader.reorder(downloads)
    }

    /** [download] first in the queue, started at once: the lowest running download waits instead. */
    fun moveToTop(download: Download) {
        downloader.moveToTop(download)
    }

    fun retryDownloads(downloads: List<Download>) {
        downloader.retry(downloads)
        startDownloads()
    }

    /** Deletes the downloads of [episodes], and cancels those queued. */
    fun deleteEpisodes(anime: Anime, episodes: List<Episode>) {
        if (episodes.isEmpty()) return
        val ids = episodes.mapTo(HashSet()) { it.id }
        downloader.remove(queueState.value.filter { it.episode.id in ids })
        scope.launch {
            try {
                val source = sourceManager.getOrStub(anime.source)
                val animeDir = provider.findAnimeDir(source, anime) ?: return@launch
                val sourceDirName = provider.getSourceDirName(source)
                val animeDirName = animeDir.name ?: provider.getAnimeDirName(anime)
                val dirs = animeDir.listFiles().orEmpty()
                val episodeDirs = DownloadNames.EpisodeDirs(dirs.mapNotNull { it.name })
                episodes.forEach { episode ->
                    val name = episodeDirs.find(episode) ?: return@forEach
                    dirs.find { it.name == name }?.deleteRecursively()
                    cache.removeEpisode(sourceDirName, animeDirName, name)
                }
                if (animeDir.listFiles().isNullOrEmpty()) {
                    animeDir.delete()
                    cache.removeAnime(sourceDirName, animeDirName)
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e, tag = TAG) { "Couldn't delete the downloads of ${anime.title}" }
            }
        }
    }

    /** Deletes every download of [anime], and cancels those queued. */
    fun deleteAnime(anime: Anime) {
        downloader.remove(queueState.value.filter { it.anime.id == anime.id })
        scope.launch {
            try {
                val source = sourceManager.getOrStub(anime.source)
                val animeDir = provider.findAnimeDir(source, anime) ?: return@launch
                val animeDirName = animeDir.name ?: provider.getAnimeDirName(anime)
                animeDir.deleteRecursively()
                cache.removeAnime(provider.getSourceDirName(source), animeDirName)
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e, tag = TAG) { "Couldn't delete the downloads of ${anime.title}" }
            }
        }
    }

    /** From the cache: no storage access. */
    fun isEpisodeDownloaded(anime: Anime, episode: Episode): Boolean {
        return DownloadNames.EpisodeDirs(downloadedDirs(anime)).find(episode) != null
    }

    /** How many episodes of [anime] are downloaded, from the cache. */
    fun getDownloadCount(anime: Anime): Int = downloadedDirs(anime).size

    /** The download state of each of [episodes], by episode id. No storage access. */
    fun getDownloadStates(anime: Anime, episodes: List<Episode>): Map<Long, EpisodeDownloadState> {
        val queued = queueState.value.filter { it.anime.id == anime.id }.associateBy { it.episode.id }
        val downloaded = DownloadNames.EpisodeDirs(downloadedDirs(anime))
        return episodes.associate { episode ->
            val download = queued[episode.id]
            episode.id to when {
                download != null -> EpisodeDownloadState(download.status, download.progress)
                downloaded.find(episode) != null ->
                    EpisodeDownloadState(Download.State.DOWNLOADED, 100)
                else -> EpisodeDownloadState()
            }
        }
    }

    /** The files of [episode] when downloaded, read from the storage, not the cache. */
    suspend fun getLocalEpisode(episode: Episode): LocalEpisode? = withContext(Dispatchers.IO) {
        try {
            val anime = animeRepository.getAnimeById(episode.animeId)
            val source = sourceManager.getOrStub(anime.source)
            provider.findEpisodeDir(source, anime, episode)?.let { LocalEpisode.from(it, it.name.orEmpty()) }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e, tag = TAG) { "Couldn't read the download of ${episode.name}" }
            null
        }
    }

    /** Runs the queue, for [DownloadWorker]. */
    internal suspend fun runQueue() = downloader.runQueue()

    private fun downloadedDirs(anime: Anime): Set<String> {
        val source = sourceManager.getOrStub(anime.source)
        return cache.episodeDirs(provider.getSourceDirName(source), provider.getAnimeDirName(anime))
    }

    /**
     * Renames source download folder
     *
     * @param oldSource the old source.
     * @param newSource the new source.
     */
    fun renameSource(oldSource: Source, newSource: Source) {
        val oldFolder = provider.findSourceDir(oldSource) ?: return
        val newName = provider.getSourceDirName(newSource)

        if (oldFolder.name == newName) return

        val capitalizationChanged = oldFolder.name.equals(newName, ignoreCase = true)
        if (capitalizationChanged) {
            val tempName = newName + TMP_DIR_SUFFIX
            if (oldFolder.renameTo(tempName).not()) {
                Log.d("RenameSource", "Failed to rename source download folder: ${oldFolder.name}")
                return
            }
        }

        if (oldFolder.renameTo(newName).not()) {
            Log.d("RenameSource", "Failed to rename source download folder: ${oldFolder.name}")
        }
        cache.rescan()
    }

    companion object {
        const val TMP_DIR_SUFFIX = "_tmp"

        /** In the app's private files, out of the backups */
        private const val DOWNLOADS_DATA_DIR = "downloads"
        private const val TAG = "TadamiDownloadManager"
    }

}
