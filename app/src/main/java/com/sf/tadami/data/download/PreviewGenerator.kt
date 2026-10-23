package com.sf.tadami.data.download

import android.content.Context
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.sf.tadami.preferences.player.PlayerPreferences
import com.sf.tadami.ui.animeinfos.episode.cast.preview.CastFrameGrabber
import com.sf.tadami.ui.animeinfos.episode.player.preview.PREVIEW_MAX_ERRORS
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewErrors
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewSidecar
import com.sf.tadami.ui.animeinfos.episode.player.preview.phonePreviewBucketOf
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketRangeMs
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketSizeMs
import com.sf.tadami.utils.getPreferencesGroup
import com.sf.tadami.utils.logcat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import logcat.LogPriority
import java.io.File
import kotlin.coroutines.resume

/** A downloaded episode whose seekbar preview frames are to be made: its folders, by name. */
@Serializable
data class PendingPreviews(val sourceDir: String, val animeDir: String, val episodeDir: String)

/** What becomes of a [PendingPreviews] when its turn comes. */
enum class PreviewGenerationDecision {
    /** Make its frames. */
    GENERATE,

    /** "Seekbar preview" or "Prefetch the previews of downloaded episodes" is off: none made. */
    SETTINGS_OFF,

    /** The episode was deleted meanwhile. */
    DELETED,

    /** Its frames are there already, valid (made by its playback, or before a process death). */
    DONE,
}

fun previewGenerationDecision(settingsOn: Boolean, videoExists: Boolean, hasValidSidecar: Boolean): PreviewGenerationDecision =
    when {
        !videoExists -> PreviewGenerationDecision.DELETED
        !settingsOn -> PreviewGenerationDecision.SETTINGS_OFF
        hasValidSidecar -> PreviewGenerationDecision.DONE
        else -> PreviewGenerationDecision.GENERATE
    }

/**
 * The episodes waiting for their frames, in download order, each once. The one whose frames are being made
 * ([start]) waits still until they're done ([finish]): it is saved, and downloaded again meanwhile it comes again.
 * Not thread-safe.
 */
class PreviewQueue(entries: List<PendingPreviews> = emptyList()) {
    private val pending = ArrayDeque(entries.distinct())
    private var started: PendingPreviews? = null

    /** The episode [start]ed was downloaded again since: what is being made is of its old file. */
    var startedDownloadedAgain = false
        private set

    val entries: List<PendingPreviews>
        get() = pending.toList()

    val first: PendingPreviews?
        get() = pending.firstOrNull()

    /** False when it waits already; the one [start]ed is then [startedDownloadedAgain]. */
    fun add(entry: PendingPreviews): Boolean {
        if (entry == started) startedDownloadedAgain = true
        if (entry in pending) return false
        pending.addLast(entry)
        return true
    }

    /** The first episode waiting, its frames being made from now on; null when none waits. */
    fun start(): PendingPreviews? {
        started = pending.firstOrNull()
        startedDownloadedAgain = false
        return started
    }

    /**
     * The frames of the episode [start]ed are done with: it leaves the queue, unless [kept] (to be made again) or
     * downloaded again meanwhile.
     */
    fun finish(kept: Boolean = false) {
        val entry = started ?: return
        if (!kept && !startedDownloadedAgain) pending.remove(entry)
        started = null
        startedDownloadedAgain = false
    }
}

/** Decodes the frames of one video, off the main thread. */
interface PreviewFrameDecoder {
    /** The video's duration as the player reads it, null when it can't be read. */
    suspend fun durationMs(): Long?

    /** A frame within [toleranceMs] of [positionMs]: a JPEG and its time, null when none came in time. */
    suspend fun frame(positionMs: Long, toleranceMs: Long): DecodedFrame?

    fun release()

    /** [jpeg] ([width]×[height]), the frame at [timeMs] of the video. */
    class DecodedFrame(val jpeg: ByteArray, val width: Int, val height: Int, val timeMs: Long)

    /** The decoder failed on [errorCode] ([PlaybackException.errorCode]): no frame of the step came. */
    class FailedException(val errorCode: Int = PlaybackException.ERROR_CODE_UNSPECIFIED) : Exception()
}

/** What [makePreviewSidecar] came to. */
sealed interface MadePreviews {
    /** The frames of the steps that gave one. */
    class Done(val sidecar: PreviewSidecar) : MadePreviews

    /** The video can't be decoded: no duration, not a single frame, or [PREVIEW_MAX_ERRORS] errors. */
    data object Failed : MadePreviews

    /** Stopped before the end: nothing made, everything to do again. */
    data object Stopped : MadePreviews
}

/**
 * The seekbar preview frames of a [videoLength]-byte video, one per step of the phone player (see
 * [phonePreviewBucketOf]), decoded in turn by a decoder from [newDecoder]. Stops before the next step once
 * [stopped], its decoder released, so the player screen or a cast has the device's decoders to itself.
 *
 * A step without a frame in time, or that failed to decode, is left out rather than kept as one without: a timeout
 * or an error may not come again, the player and the cast fetch it when it's needed. A decoder taken back or that
 * couldn't start tries its step again; the errors are counted as the player's preview counts them ([PreviewErrors]):
 * the [PREVIEW_MAX_ERRORS]th gives up on the video.
 */
suspend fun makePreviewSidecar(
    videoLength: Long,
    newDecoder: () -> PreviewFrameDecoder,
    stopped: () -> Boolean = { false },
): MadePreviews {
    if (stopped()) return MadePreviews.Stopped
    val decoder = newDecoder()
    val errors = PreviewErrors()

    // The frame of the step [range], null when it gives none; throws once the video is given up
    suspend fun frameOf(range: LongRange): PreviewFrameDecoder.DecodedFrame? {
        while (true) {
            try {
                return decoder.frame((range.first + range.last + 1) / 2, (range.last + 1 - range.first) / 2)
            } catch (e: PreviewFrameDecoder.FailedException) {
                val outcome = errors.onError(e.errorCode)
                if (outcome.failSource) throw e
                if (outcome.markStepUnavailable) return null
            }
        }
    }

    try {
        val durationMs = decoder.durationMs()?.takeIf { it > 0L } ?: return MadePreviews.Failed
        val last = phonePreviewBucketOf(durationMs - 1, durationMs)
        var width = 0
        var height = 0
        val entries = mutableListOf<PreviewSidecar.Entry>()
        for (step in 0..last) {
            if (stopped()) return MadePreviews.Stopped
            val frame = try {
                frameOf(previewBucketRangeMs(step, durationMs))
            } catch (_: PreviewFrameDecoder.FailedException) {
                return MadePreviews.Failed
            } ?: continue
            if (width == 0) {
                width = frame.width
                height = frame.height
            }
            entries += PreviewSidecar.Entry(step, frame.timeMs, frame.jpeg)
        }
        if (width == 0) return MadePreviews.Failed
        return MadePreviews.Done(PreviewSidecar(videoLength, durationMs, previewBucketSizeMs(durationMs), width, height, entries))
    } finally {
        decoder.release()
    }
}

/**
 * Makes the seekbar preview frames of each episode right after its download, in the background, and keeps them
 * next to it ([EpisodePreviews]), so its first scrub, on the phone or the TV, shows them at once.
 *
 * - One episode at a time, in download order, with the cast's headless decoder ([CastFrameGrabber]) reading the
 *   file; it never touches the download itself (done, its status set, before it is queued).
 * - In a background job ([PreviewWorker], [schedule]), [makeAll] running it: the process is neither frozen nor
 *   killed meanwhile, with the app and the downloads' service gone, and WorkManager runs it again after a stop.
 * - Nothing when "Seekbar preview" or "Prefetch the previews of downloaded episodes" is off, nor for an episode
 *   deleted meanwhile, nor when its frames are there already.
 * - Stopped while the player screen is in front or plays ([playerActive]), or a cast runs ([castActive]): the
 *   decoder is released, the playback keeps the device's decoders and CPU. The episode it was on is made again
 *   once they're gone, unless its frames were made meanwhile (by the playback, the cast).
 * - The episodes waiting survive a process death ([store]): their frames are made in the next process. A download
 *   whose frames still aren't there makes them at its first playback instead (the player's prefetch).
 */
class PreviewGenerator(
    private val store: File,
    /** "Seekbar preview" and "Prefetch the previews of downloaded episodes" are both on. */
    private val settingsOn: suspend () -> Boolean,
    /** Where the frames of the episode waiting go, null once its folder is gone. Storage access. */
    private val previewsOf: (PendingPreviews) -> EpisodePreviews?,
    /** A decoder of the episode's video. */
    private val newDecoder: (EpisodePreviews) -> PreviewFrameDecoder,
    /** Writes the frames next to the episode, unless some are there already. */
    private val write: (EpisodePreviews, PreviewSidecar) -> Unit,
    /** Has [makeAll] run in the background soon, after the run in progress if any. */
    private val schedule: () -> Unit,
    /** True while the player screen or a cast is active: no frame is made meanwhile. */
    private val paused: StateFlow<Boolean>,
    scope: CoroutineScope,
) {
    private val queue = PreviewQueue(restore())
    private val lock = Any()

    // A run is scheduled or runs: a new episode waiting needs no other
    private var scheduled = false
    private val running = Mutex()

    init {
        // The episodes waiting have their turn once nothing pauses them: right away for those of an earlier process
        scope.launch { paused.collect { if (!it) scheduleIfWaiting() } }
    }

    /** The episodes waiting, in turn. */
    internal val waiting: List<PendingPreviews>
        get() = synchronized(lock) { queue.entries }

    /** The episode in [episodeDir] (of [animeDir], [sourceDir]) was just downloaded: its frames are made in turn. */
    fun onDownloaded(sourceDir: String, animeDir: String, episodeDir: String) {
        val added = synchronized(lock) { queue.add(PendingPreviews(sourceDir, animeDir, episodeDir)) }
        if (!added) return
        save()
        scheduleIfWaiting()
    }

    /**
     * Makes the frames of the episodes waiting, one at a time, until none waits or the player screen or a cast is
     * active: the episode it was on then waits still. An episode downloaded again meanwhile is made again, from its
     * new file. For [PreviewWorker].
     */
    suspend fun makeAll() = running.withLock {
        try {
            while (true) {
                val next = synchronized(lock) { if (paused.value) null else queue.start() } ?: break
                val done = try {
                    process(next)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.WARN, e, tag = TAG) { "Couldn't make the preview frames of ${next.episodeDir}" }
                    true
                }
                synchronized(lock) { queue.finish(kept = !done) }
                save()
            }
        } finally {
            // Stopped: the episode it was on waits still
            synchronized(lock) {
                queue.finish(kept = true)
                scheduled = false
            }
            // What came meanwhile (an episode downloaded as the run ended, the end of a pause) has its own run
            scheduleIfWaiting()
        }
    }

    private fun scheduleIfWaiting() {
        val start = synchronized(lock) {
            (!scheduled && !paused.value && queue.first != null).also { if (it) scheduled = true }
        }
        if (start) schedule()
    }

    /**
     * Makes the frames of [entry], or finds there's nothing to make: false when stopped, the episode to make again
     * (the player screen or a cast is active, or it was downloaded again: its file changed under the decoder).
     */
    private suspend fun process(entry: PendingPreviews): Boolean {
        fun downloadedAgain() = synchronized(lock) { queue.startedDownloadedAgain }
        val previews = previewsOf(entry)
        val decision = previewGenerationDecision(
            settingsOn = settingsOn(),
            videoExists = previews?.video?.exists() == true,
            // Made meanwhile, by its playback or a cast while this was stopped: nothing more to do
            hasValidSidecar = previews?.read() != null,
        )
        if (decision != PreviewGenerationDecision.GENERATE || previews == null) {
            logcat(tag = TAG) { "Preview frames of ${entry.episodeDir}: ${decision.name.lowercase()}" }
            return true
        }
        val startedAt = SystemClock.elapsedRealtime()
        logcat(tag = TAG) { "Making the preview frames of ${entry.episodeDir}" }
        val stopped = { paused.value || downloadedAgain() }
        when (val made = makePreviewSidecar(previews.video.length(), { newDecoder(previews) }, stopped)) {
            MadePreviews.Stopped -> {
                logcat(tag = TAG) {
                    "Stopped, ${if (downloadedAgain()) "downloaded again" else "the player screen or a cast is active"}: " +
                        "${entry.episodeDir} made again"
                }
                return false
            }
            MadePreviews.Failed -> {
                logcat(LogPriority.WARN, tag = TAG) { "No preview frames for ${entry.episodeDir}: it can't be decoded" }
                return true
            }
            is MadePreviews.Done -> {
                val sidecar = made.sidecar
                logcat(tag = TAG) {
                    val steps = phonePreviewBucketOf(sidecar.durationMs - 1, sidecar.durationMs) + 1
                    "Made ${sidecar.frameCount} preview frames of ${entry.episodeDir}, ${steps - sidecar.frameCount} " +
                        "steps without, in ${(SystemClock.elapsedRealtime() - startedAt) / 1000} s"
                }
                // Of the old file: made again from the new one
                if (downloadedAgain()) return false
                write(previews, sidecar)
                return true
            }
        }
    }

    private fun restore(): List<PendingPreviews> {
        return try {
            if (!store.exists()) return emptyList()
            DownloadStore.DEFAULT_JSON.decodeFromString<List<PendingPreviews>>(store.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    // The queue as it is, written before any later change of it: two saves never mix nor go back in time
    private fun save() = synchronized(lock) {
        val entries = queue.entries
        if (entries.isEmpty()) store.delete() else StoreFiles.writeAtomically(store, DownloadStore.DEFAULT_JSON.encodeToString(entries))
    }

    /** [CastFrameGrabber] on the file at [videoUri], read as the phone player reads a download. */
    @OptIn(UnstableApi::class)
    private class GrabberDecoder(context: Context, videoUri: String) : PreviewFrameDecoder {
        private val grabber = CastFrameGrabber(context).apply {
            setSource { DefaultMediaSourceFactory(context).createMediaSource(MediaItem.fromUri(videoUri)) }
        }

        override suspend fun durationMs(): Long? = suspendCancellableCoroutine { continuation ->
            grabber.duration { continuation.resume(it) }
        }

        override suspend fun frame(positionMs: Long, toleranceMs: Long): PreviewFrameDecoder.DecodedFrame? {
            val result = suspendCancellableCoroutine { continuation ->
                grabber.grab(positionMs, toleranceMs) { continuation.resume(it) }
            }
            return when (result) {
                is CastFrameGrabber.Result.Frame ->
                    PreviewFrameDecoder.DecodedFrame(result.jpeg.bytes, result.jpeg.width, result.jpeg.height, result.timeMs)
                CastFrameGrabber.Result.NoFrame -> null
                is CastFrameGrabber.Result.Failed -> throw PreviewFrameDecoder.FailedException(result.errorCode)
            }
        }

        override fun release() = grabber.release()
    }

    companion object {
        private const val TAG = "PreviewGenerator"

        /**
         * True while the phone's player screen is in front (PiP included) or plays: no frame is made, so the
         * playback has the decoders and the CPU. Set by the player screen.
         */
        val playerActive = MutableStateFlow(false)

        /**
         * True during a cast session (its service runs): no frame is made, the cast's own preview frames and its
         * proxy come first. Set by the cast service.
         */
        val castActive = MutableStateFlow(false)

        /** The generator of the downloads in [provider], the episodes waiting kept in [store]. */
        fun create(
            context: Context,
            provider: DownloadProvider,
            store: File,
            dataStore: DataStore<Preferences>,
            scope: CoroutineScope,
        ) = PreviewGenerator(
            store = store,
            settingsOn = { dataStore.getPreferencesGroup(PlayerPreferences).let { it.seekPreview && it.seekPreviewPrefetch } },
            previewsOf = { entry ->
                provider.downloadsDir?.findFile(entry.sourceDir)?.findFile(entry.animeDir)?.findFile(entry.episodeDir)
                    ?.let { LocalEpisode.from(it, entry.episodeDir) }?.previews
            },
            newDecoder = { previews -> GrabberDecoder(context, previews.video.uri.toString()) },
            write = { previews, sidecar -> previews.write(context, sidecar) },
            schedule = { PreviewWorker.start(context) },
            paused = combine(playerActive, castActive) { player, cast -> player || cast }
                .stateIn(scope, SharingStarted.Eagerly, playerActive.value || castActive.value),
            scope = scope,
        )
    }
}
