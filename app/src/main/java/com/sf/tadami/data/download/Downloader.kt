package com.sf.tadami.data.download

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.hippo.unifile.UniFile
import com.sf.tadami.R
import com.sf.tadami.data.anime.AnimeRepository
import com.sf.tadami.data.download.model.Download
import com.sf.tadami.data.download.stream.DownloadFatalException
import com.sf.tadami.data.download.stream.DownloadOutput
import com.sf.tadami.data.download.stream.FailureAction
import com.sf.tadami.data.download.stream.MediaCheck
import com.sf.tadami.data.download.stream.NetworkLostException
import com.sf.tadami.data.download.stream.StorageException
import com.sf.tadami.data.download.stream.StreamDownloader
import com.sf.tadami.data.download.stream.StreamFailedException
import com.sf.tadami.data.download.stream.StreamFallback
import com.sf.tadami.data.download.stream.StreamFailure
import com.sf.tadami.data.episode.EpisodeRepository
import com.sf.tadami.domain.episode.toSEpisode
import com.sf.tadami.extension.util.callGuarded
import com.sf.tadami.network.HttpException
import com.sf.tadami.network.NetworkHelper
import com.sf.tadami.preferences.download.DownloadPreferences
import com.sf.tadami.source.StubSource
import com.sf.tadami.ui.tabs.browse.SourceManager
import com.sf.tadami.ui.utils.formattedMessage
import com.sf.tadami.utils.DiskUtil
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.isOnline
import com.sf.tadami.utils.logcat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Runs the download queue: [parallel slots][DownloadPreferences.parallelDownloads], at most one
 * active download per source so the hosts don't ban the user. Each episode tries its streams in
 * the extension's order until one gives a valid video, see [StreamFallback]. Doesn't depend on
 * how it is scheduled: [runQueue] runs until nothing is left to start.
 */
class Downloader(
    private val context: Context,
    private val provider: DownloadProvider,
    private val cache: DownloadCache,
    private val store: DownloadStore,
    private val resumeStore: ResumeStore,
    private val scope: CoroutineScope,
) {

    private val sourceManager: SourceManager by injectLazy()
    private val episodeRepository: EpisodeRepository by injectLazy()
    private val animeRepository: AnimeRepository by injectLazy()
    private val dataStore: DataStore<Preferences> by injectLazy()

    private val streamDownloader by lazy {
        val client = Injekt.get<NetworkHelper>().client.newBuilder()
            .cache(null)
            // One call can last as long as a whole progressive video
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        StreamDownloader(context, client)
    }

    private val _queueState = MutableStateFlow<List<Download>>(emptyList())
    val queueState: StateFlow<List<Download>> = _queueState.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /** The queue changed or a download ended: the runner looks for what to start */
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val activeJobs = ConcurrentHashMap<Download, Job>()
    private val lock = Any()
    private val restored = CompletableDeferred<Unit>()

    /** The slots of the running queue, read by the runner before it starts downloads */
    @Volatile
    private var parallel = 1

    // In order, off the caller's thread
    private val persistDispatcher = Dispatchers.IO.limitedParallelism(1)

    init {
        scope.launch { restore() }
    }

    suspend fun awaitRestored() = restored.await()

    private suspend fun restore() {
        try {
            val entries = store.restore()
            // Two queries per anime, not per episode: the queue shows up sooner after a cold start
            val episodes = entries.map { it.animeId }.distinct().mapNotNull { animeId ->
                try {
                    val anime = animeRepository.getAnimeById(animeId)
                    anime to episodeRepository.getEpisodesByAnimeId(animeId).associateBy { it.id }
                } catch (e: Exception) {
                    // Deleted meanwhile
                    null
                }
            }.associateBy { it.first.id }
            val found = entries.mapNotNull { entry ->
                val (anime, animeEpisodes) = episodes[entry.animeId] ?: return@mapNotNull null
                val episode = animeEpisodes[entry.episodeId] ?: return@mapNotNull null
                Download(sourceManager.getOrStub(entry.sourceId), anime, episode, entry.dirName)
            }
            // Completed just before the process died, while the queue was being saved
            val firstOfAnime = found.associateBy { it.anime.id }
            val downloads = DownloadStore.withoutDownloaded(found, { it.episode }) { animeId ->
                firstOfAnime[animeId]?.let(::episodeDirsOf).orEmpty()
            }
            (found - downloads.toSet()).forEach {
                logcat { "${it.episode.name} of ${it.anime.title} is downloaded already, not restored" }
                resumeStore.delete(it.episode.id)
            }
            synchronized(lock) {
                val current = _queueState.value
                val queued = current.mapTo(HashSet()) { it.episode.id }
                setQueue(downloads.filter { it.episode.id !in queued } + current)
            }
        } finally {
            restored.complete(Unit)
        }
    }

    /** Adds [downloads] at the end of the queue, the episodes already queued left out. */
    fun add(downloads: List<Download>) {
        synchronized(lock) {
            val queued = _queueState.value.mapTo(HashSet()) { it.episode.id }
            val added = downloads.filter { queued.add(it.episode.id) }
            if (added.isEmpty()) return
            added.forEach { it.status = Download.State.QUEUED }
            setQueue(_queueState.value + added)
        }
        wake.trySend(Unit)
    }

    /** Removes [downloads] from the queue, stops them, and deletes what they had downloaded. */
    fun remove(downloads: List<Download>) {
        if (downloads.isEmpty()) return
        val removed = downloads.toSet()
        synchronized(lock) {
            setQueue(_queueState.value.filterNot { it in removed })
        }
        removed.forEach { download ->
            download.status = Download.State.NOT_DOWNLOADED
            val job = activeJobs.remove(download)
            job?.cancel()
            scope.launch {
                job?.join()
                deleteTmp(download)
            }
        }
        wake.trySend(Unit)
    }

    /** The queue in the order of [downloads], those missing from it at the end. */
    fun reorder(downloads: List<Download>) {
        synchronized(lock) {
            val current = _queueState.value
            val ordered = downloads.filter { it in current }.distinct()
            setQueue(ordered + current.filterNot { it in ordered })
        }
    }

    /**
     * [download] first in the queue. When it waits for a slot or for its source, the lowest running
     * downloads stop where they are so it starts now: they stay queued, with their stream and their
     * progress, and resume once a slot frees up.
     */
    fun moveToTop(download: Download) {
        val preempted = synchronized(lock) {
            setQueue(QueueOrder.toTop(_queueState.value, listOf(download)))
            if (!_isRunning.value || download.status != Download.State.QUEUED) return
            val running = activeJobs.filterValues { it.isActive }.keys
            QueueOrder.toPreempt(_queueState.value, running, parallel, download) { it.source.id }
        }
        preempted.forEach { activeJobs[it]?.cancel() }
        wake.trySend(Unit)
    }

    /** Failed [downloads] back in the queue. */
    fun retry(downloads: List<Download>) {
        downloads.filter { it.status == Download.State.ERROR }.forEach {
            it.error = null
            it.progress = 0
            it.videoProgress = null
            it.status = Download.State.QUEUED
        }
        wake.trySend(Unit)
    }

    /** Whether the runner runs and will see the queue changes; false when it must be started. */
    fun signalIfRunning(): Boolean {
        synchronized(lock) {
            if (!_isRunning.value) return false
            wake.trySend(Unit)
            return true
        }
    }

    /** The saves run in order: the returned job ends once this queue is written. */
    private fun setQueue(downloads: List<Download>): Job {
        _queueState.value = downloads
        val entries = downloads.map { DownloadStore.Entry(it.episode.id, it.anime.id, it.source.id, it.dirName) }
        return scope.launch(persistDispatcher) { store.save(entries) }
    }

    /**
     * Downloads the queue until nothing is left to start: everything downloaded or failed.
     * Cancelling it stops the downloads where they are: they stay queued, with their resume data.
     */
    suspend fun runQueue() = coroutineScope {
        restored.await()
        synchronized(lock) { _isRunning.value = true }
        try {
            val running = mutableMapOf<Download, Job>()
            while (true) {
                running.entries.removeAll { !it.value.isActive }
                parallel = dataStore.getPreferencesGroup(DownloadPreferences).parallelDownloads
                val done = synchronized(lock) {
                    val busySources = running.keys.mapTo(HashSet()) { it.source.id }
                    for (download in _queueState.value) {
                        if (running.size >= parallel) break
                        if (download.status != Download.State.QUEUED || download in running) continue
                        if (!busySources.add(download.source.id)) continue
                        val job = launch(Dispatchers.IO) { process(download) }
                        running[download] = job
                        activeJobs[download] = job
                        job.invokeOnCompletion {
                            activeJobs.remove(download, job)
                            wake.trySend(Unit)
                        }
                    }
                    // Atomic with the check of signalIfRunning: a download added now starts a new run
                    if (running.isEmpty()) _isRunning.value = false
                    running.isEmpty()
                }
                if (done) break
                wake.receive()
            }
        } finally {
            _isRunning.value = false
        }
    }

    private suspend fun process(download: Download) {
        download.error = null
        download.status = Download.State.DOWNLOADING
        try {
            while (true) {
                try {
                    downloadEpisode(download)
                    break
                } catch (e: NetworkLostException) {
                    // Resumes on the same stream once the network is back
                    download.status = Download.State.QUEUED
                    while (!context.isOnline()) delay(NETWORK_CHECK_DELAY_MS)
                    download.status = Download.State.DOWNLOADING
                }
            }
        } catch (e: Exception) {
            // Paused or stopped, the failure coming from the stop: still queued, unless removed
            if (e is CancellationException || !currentCoroutineContext().isActive) {
                if (download.status == Download.State.DOWNLOADING) download.status = Download.State.QUEUED
                throw e as? CancellationException ?: CancellationException("Stopped").apply { initCause(e) }
            }
            logcat(LogPriority.WARN, e) { "Couldn't download ${download.anime.title} - ${download.episode.name}" }
            download.error = errorMessage(e)
            download.status = Download.State.ERROR
        }
    }

    private suspend fun downloadEpisode(download: Download) {
        val source = download.source
        if (source is StubSource) {
            throw DownloadFatalException(context.getString(R.string.source_not_installed, source.name))
        }
        val noStorage = { DownloadFatalException(context.getString(R.string.download_error_no_storage)) }
        val animeDir = DownloadOutput.storage { provider.getOrCreateAnimeDir(source, download.anime) }
            ?: throw noStorage()
        val available = DiskUtil.getAvailableStorageSpace(animeDir)
        if (available in 0 until MIN_FREE_SPACE_BYTES) {
            throw DownloadFatalException(context.getString(R.string.download_error_no_space))
        }
        val tmpDir = DownloadOutput.storage { animeDir.createDirectory(DownloadNames.tmpDirName(download.dirName)) }
            ?: throw noStorage()

        val streams = try {
            source.callGuarded { getEpisodeSources(download.episode.toSEpisode()) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            if (!context.isOnline()) throw NetworkLostException(e)
            throw e
        }
        if (streams.isEmpty()) throw DownloadFatalException(context.getString(R.string.download_error_no_stream))

        val episodeId = download.episode.id
        var resume = resumeStore.load(episodeId) ?: ResumeData()
        var lastError: Throwable? = null
        for (stream in StreamFallback.attemptOrder(streams, resume.stream, resume.failed)) {
            val key = StreamFallback.key(stream)
            if (resume.stream != key) {
                // Left by another stream
                clearDir(tmpDir)
                resume = resume.copy(stream = key, files = emptyMap())
                resumeStore.save(episodeId, resume)
            }
            try {
                val downloaded = streamDownloader.download(
                    stream = stream,
                    dir = tmpDir,
                    episodeDirName = download.dirName,
                    resume = resume.files,
                    onFileProgress = { role, file ->
                        // Each segment or 2 MiB: stop before the device is full, not at its last byte
                        if (DiskUtil.getAvailableStorageSpace(animeDir) in 0 until MIN_FREE_SPACE_BYTES) {
                            throw DownloadFatalException(context.getString(R.string.download_error_no_space))
                        }
                        if (role == StreamDownloader.VIDEO) download.videoProgress = file
                        resume = resume.copy(files = resume.files + (role to file))
                        resumeStore.save(episodeId, resume)
                    },
                    onProgress = { download.progress = (it * 100).toInt().coerceIn(0, 99) },
                )
                checkDuration(downloaded.video.file)
                // In the tmp dir: the episode dir appears with its sidecar or not at all
                EpisodeMetadata.write(
                    context,
                    tmpDir,
                    download.dirName,
                    EpisodeMetadata.of(stream, downloaded.video, downloaded.audios),
                )
                finish(download, animeDir, tmpDir)
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Failed because it was stopped: keep the stream and its progress
                currentCoroutineContext().ensureActive()
                when (StreamFallback.classify(e, context.isOnline())) {
                    FailureAction.WAIT_FOR_NETWORK -> throw e as? NetworkLostException ?: NetworkLostException(e)
                    FailureAction.STOP -> throw e
                    FailureAction.NEXT_STREAM -> {
                        logcat(LogPriority.WARN, e) { "Stream ${stream.fullName} failed for ${download.episode.name}" }
                        lastError = e
                        resume = resume.copy(stream = null, failed = (resume.failed + key).distinct(), files = emptyMap())
                        resumeStore.save(episodeId, resume)
                        clearDir(tmpDir)
                        download.progress = 0
                        download.videoProgress = null
                    }
                }
            }
        }
        val reason = lastError?.let(::errorMessage).orEmpty()
        throw DownloadFatalException(context.getString(R.string.download_error_all_streams_failed, reason), lastError)
    }

    // In the user's language: the exception messages stay in English, for the logs
    private fun errorMessage(error: Throwable): String {
        return when (val cause = StreamFallback.unwrap(error)) {
            is StreamFailedException -> context.getString(cause.failure.text)
            // The system's own message is in English
            is StorageException -> context.getString(R.string.download_error_storage)
            // Ours are written for the user
            is DownloadFatalException -> cause.message.orEmpty()
            is SocketTimeoutException -> context.getString(R.string.download_error_timeout)
            is UnknownHostException -> context.getString(R.string.download_error_unknown_host)
            is HttpException -> context.getString(R.string.download_error_http, cause.code)
            else -> with(context) { error.formattedMessage }
        }
    }

    /** A placeholder clip passes the size check sometimes, not the duration one. TS often has none. */
    private fun checkDuration(video: UniFile) {
        val durationMs = try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, video.uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } finally {
                retriever.release()
            }
        } catch (e: Exception) {
            null
        }
        if (MediaCheck.isTooShort(durationMs)) {
            throw StreamFailedException(StreamFailure.TOO_SHORT, "The video lasts ${durationMs!! / 1000} s only")
        }
    }

    /**
     * The tmp dir becomes the episode dir: only now is the episode seen as downloaded. It leaves the
     * saved queue at once, so a process dying now doesn't download it again (see [restore] too).
     */
    private suspend fun finish(download: Download, animeDir: UniFile, tmpDir: UniFile) {
        DownloadOutput.storage {
            animeDir.findFile(download.dirName)?.deleteRecursively()
            if (!tmpDir.renameTo(download.dirName)) throw IOException("Can't rename ${tmpDir.name}")
        }
        cache.addEpisode(
            provider.getSourceDirName(download.source),
            animeDir.name ?: provider.getAnimeDirName(download.anime),
            download.dirName,
        )
        val saved = synchronized(lock) { setQueue(_queueState.value - download) }
        resumeStore.delete(download.episode.id)
        download.progress = 100
        // Before the wait: a pause now doesn't turn it back to queued
        download.status = Download.State.DOWNLOADED
        withContext(NonCancellable) { saved.join() }
    }

    /** The episode dirs in the anime dir of [download], read from the storage: the cache may not be filled yet. */
    private fun episodeDirsOf(download: Download): List<String> {
        return try {
            provider.findAnimeDir(download.source, download.anime)?.listFiles().orEmpty()
                .filter { it.isDirectory }
                .mapNotNull { it.name }
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Couldn't read the downloads of ${download.anime.title}" }
            emptyList()
        }
    }

    private fun clearDir(dir: UniFile) {
        DownloadOutput.storage { dir.listFiles()?.forEach { it.deleteRecursively() } }
    }

    private fun deleteTmp(download: Download) {
        resumeStore.delete(download.episode.id)
        try {
            val animeDir = provider.findAnimeDir(download.source, download.anime) ?: return
            animeDir.findFile(DownloadNames.tmpDirName(download.dirName))?.deleteRecursively()
            if (animeDir.listFiles().isNullOrEmpty()) animeDir.delete()
        } catch (e: Exception) {
            logcat(LogPriority.WARN, e) { "Couldn't delete the partial download of ${download.episode.name}" }
        }
    }

    private companion object {
        const val MIN_FREE_SPACE_BYTES = 200L * 1024 * 1024
        const val NETWORK_CHECK_DELAY_MS = 5_000L
    }
}

/** Deletes a file, or a directory with its content: not every [UniFile] does it for a directory. */
fun UniFile.deleteRecursively(): Boolean {
    if (isDirectory) listFiles()?.forEach { it.deleteRecursively() }
    return delete()
}
