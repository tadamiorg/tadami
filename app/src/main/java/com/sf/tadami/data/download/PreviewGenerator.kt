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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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

/** The episodes waiting for their frames, in download order, each once. Not thread-safe. */
class PreviewQueue(entries: List<PendingPreviews> = emptyList()) {
    private val pending = ArrayDeque(entries.distinct())

    val entries: List<PendingPreviews>
        get() = pending.toList()

    val first: PendingPreviews?
        get() = pending.firstOrNull()

    /** False when it waits already. */
    fun add(entry: PendingPreviews): Boolean {
        if (entry in pending) return false
        pending.addLast(entry)
        return true
    }

    fun remove(entry: PendingPreviews) {
        pending.remove(entry)
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

/**
 * The seekbar preview frames of a [videoLength]-byte video, one per step of the phone player (see
 * [phonePreviewBucketOf]), decoded in turn by a decoder from [newDecoder]; null when the video can't be decoded.
 * Waits while [paused], its decoder released meanwhile, so the player screen has the decoder to itself.
 *
 * A step without a frame in time, or that failed to decode, is left out rather than kept as one without: a timeout
 * or an error may not come again, the player and the cast fetch it when it's needed. A decoder taken back or that
 * couldn't start tries its step again; the errors are counted as the player's preview counts them ([PreviewErrors]):
 * the [PREVIEW_MAX_ERRORS]th gives up on the video.
 */
suspend fun makePreviewSidecar(
    videoLength: Long,
    newDecoder: () -> PreviewFrameDecoder,
    paused: StateFlow<Boolean>,
    onPause: () -> Unit = {},
): PreviewSidecar? {
    var decoder: PreviewFrameDecoder? = null
    suspend fun ready(): PreviewFrameDecoder {
        if (paused.value) {
            decoder?.release()
            decoder = null
            onPause()
            paused.first { !it }
        }
        return decoder ?: newDecoder().also { decoder = it }
    }
    val errors = PreviewErrors()

    // The frame of the step [range], null when it gives none; throws once the video is given up
    suspend fun frameOf(range: LongRange): PreviewFrameDecoder.DecodedFrame? {
        while (true) {
            try {
                return ready().frame((range.first + range.last + 1) / 2, (range.last + 1 - range.first) / 2)
            } catch (e: PreviewFrameDecoder.FailedException) {
                val outcome = errors.onError(e.errorCode)
                if (outcome.failSource) throw e
                if (outcome.markStepUnavailable) return null
            }
        }
    }

    try {
        val durationMs = ready().durationMs()?.takeIf { it > 0L } ?: return null
        val last = phonePreviewBucketOf(durationMs - 1, durationMs)
        var width = 0
        var height = 0
        val entries = mutableListOf<PreviewSidecar.Entry>()
        for (step in 0..last) {
            val frame = try {
                frameOf(previewBucketRangeMs(step, durationMs))
            } catch (_: PreviewFrameDecoder.FailedException) {
                return null
            } ?: continue
            if (width == 0) {
                width = frame.width
                height = frame.height
            }
            entries += PreviewSidecar.Entry(step, frame.timeMs, frame.jpeg)
        }
        if (width == 0) return null
        return PreviewSidecar(videoLength, durationMs, previewBucketSizeMs(durationMs), width, height, entries)
    } finally {
        decoder?.release()
    }
}

/**
 * Makes the seekbar preview frames of each episode right after its download, in the background, and keeps them
 * next to it ([EpisodePreviews]), so its first scrub, on the phone or the TV, shows them at once.
 *
 * - One episode at a time, in download order, with the cast's headless decoder ([CastFrameGrabber]) reading the
 *   file; it never touches the download itself (done, its status set, before it is queued).
 * - Nothing when "Seekbar preview" or "Prefetch the previews of downloaded episodes" is off, nor for an episode
 *   deleted meanwhile, nor when its frames are there already.
 * - Paused while the player screen is in front or plays ([playerActive]): the decoder is released, the
 *   playback keeps the device's decoders and CPU; it goes on where it was once the screen is gone.
 * - The episodes waiting survive a process death ([store]): their frames are made at the next start. A download
 *   whose frames still aren't there makes them at its first playback instead (the player's prefetch).
 */
@OptIn(UnstableApi::class)
class PreviewGenerator(
    private val context: Context,
    private val provider: DownloadProvider,
    private val store: File,
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope,
) {
    private val queue = PreviewQueue(restore())
    private val lock = Any()
    private val wake = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch(Dispatchers.IO) {
            wake.send(Unit)
            for (signal in wake) {
                while (true) {
                    val next = synchronized(lock) { queue.first } ?: break
                    try {
                        process(next)
                    } catch (e: Exception) {
                        logcat(LogPriority.WARN, e, tag = TAG) { "Couldn't make the preview frames of ${next.episodeDir}" }
                    }
                    synchronized(lock) { queue.remove(next) }
                    save()
                }
            }
        }
    }

    /** The episode in [episodeDir] (of [animeDir], [sourceDir]) was just downloaded: its frames are made in turn. */
    fun onDownloaded(sourceDir: String, animeDir: String, episodeDir: String) {
        val added = synchronized(lock) { queue.add(PendingPreviews(sourceDir, animeDir, episodeDir)) }
        if (!added) return
        save()
        wake.trySend(Unit)
    }

    private suspend fun process(entry: PendingPreviews) {
        val prefs = dataStore.getPreferencesGroup(PlayerPreferences)
        val dir = provider.downloadsDir?.findFile(entry.sourceDir)?.findFile(entry.animeDir)?.findFile(entry.episodeDir)
        val previews = dir?.let { LocalEpisode.from(it, entry.episodeDir) }?.previews
        val decision = previewGenerationDecision(
            settingsOn = prefs.seekPreview && prefs.seekPreviewPrefetch,
            videoExists = previews?.video?.exists() == true,
            hasValidSidecar = previews?.read() != null,
        )
        if (decision != PreviewGenerationDecision.GENERATE || previews == null) {
            logcat(tag = TAG) { "Preview frames of ${entry.episodeDir}: ${decision.name.lowercase()}" }
            return
        }
        val startedAt = SystemClock.elapsedRealtime()
        logcat(tag = TAG) { "Making the preview frames of ${entry.episodeDir}" }
        val videoUri = previews.video.uri.toString()
        val sidecar = makePreviewSidecar(
            videoLength = previews.video.length(),
            newDecoder = { GrabberDecoder(context, videoUri) },
            paused = playerActive,
            onPause = { logcat(tag = TAG) { "Paused while the player screen is active" } },
        )
        if (sidecar == null) {
            logcat(LogPriority.WARN, tag = TAG) { "No preview frames for ${entry.episodeDir}: it can't be decoded" }
            return
        }
        logcat(tag = TAG) {
            val steps = phonePreviewBucketOf(sidecar.durationMs - 1, sidecar.durationMs) + 1
            "Made ${sidecar.frameCount} preview frames of ${entry.episodeDir}, ${steps - sidecar.frameCount} " +
                "steps without, in ${(SystemClock.elapsedRealtime() - startedAt) / 1000} s"
        }
        previews.write(context, sidecar)
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
         * True while the phone's player screen is in front (PiP included) or plays: the frames wait, so the
         * playback has the decoders and the CPU. Set by the player screen.
         */
        val playerActive = MutableStateFlow(false)
    }
}
