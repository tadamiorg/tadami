package com.sf.tadami.ui.animeinfos.episode.cast.preview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewPrefetch
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketOf
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketRangeMs
import com.sf.tadami.ui.animeinfos.episode.player.preview.shouldDropPreviewFrames

/**
 * The cast session's seekbar preview of a downloaded episode: frames of the file the phone serves to the TV,
 * decoded on the phone ([CastFrameGrabber]), cached for the session ([CastPreviewFrames]) and sent to the web
 * receiver. Owned by the cast service, so it works with the player screen closed. A streamed episode has none,
 * as in the phone player: the phone doesn't announce previews for it, refuses its requests ("off") and never
 * reads it, so its scrubs show the time alone and cost no data.
 *
 * - Prefetch: once the receiver announces its stream ([onSource]) and the TV has played steadily for
 *   a few seconds, every step is decoded in the background, nearest the playback position first
 *   ([PreviewPrefetch]), and pushed to the receiver unsolicited, paced on the cast channel.
 * - Scrub: a step the receiver lacks is asked for ([onRequest]) and decoded before any prefetch.
 *
 * The file is the one behind the proxy `/local-` URL the receiver names ([localFileUri]). The decoder is
 * released after [IDLE_RELEASE_MS] without decoding, and everything with [destroy] at the end of the session.
 * Main thread only.
 */
@UnstableApi
class CastSeekPreview(
    private val context: Context,
    /** Sends a message on the control channel. */
    private val send: (String) -> Unit,
    /** The downloaded file a proxy `/local-` URL serves, null for any other URL. */
    private val localFileUri: (String) -> String?,
    /** The receiver announced the seekPreview capability. */
    private val receiverCapable: () -> Boolean,
    /** The TV's playback (the receiver's MediaStatus), null without media. */
    private val playback: () -> CastPlayback?,
    private val newGrabber: () -> CastFrameGrabber = { CastFrameGrabber(context) },
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    /**
     * The preferences that apply, kept up to date by the owner: a change applies at the next prefetch
     * tick (within a second), no reconnection needed. [prefetch] only gates the background prefetch,
     * the scrubs' requests are answered either way.
     */
    data class Settings(
        val enabled: Boolean = false,
        val prefetch: Boolean = true,
    )

    /** The TV's player state and position. */
    data class CastPlayback(val state: PreviewPrefetch.Playback?, val positionMs: Long)

    /** Where the previews of the current stream are. */
    data class Stats(
        val sourceId: String?,
        val steps: Int,
        val frames: Int,
        val withoutFrame: Int,
        val prefetchMs: Long?,
        val complete: Boolean,
    )

    @Volatile
    var settings = Settings()

    private val frames = CastPreviewFrames()
    private val prefetch = PreviewPrefetch(clock)
    private val handler = Handler(Looper.getMainLooper())
    private val idleRelease = Runnable { releaseGrabber() }
    private val tick = Runnable { onTick() }
    private val pushNext = Runnable { onPush() }
    private var grabber: CastFrameGrabber? = null
    private var grabberSourceId: String? = null

    // The stream the receiver plays, from its announcement (or its last request)
    private var source: CastPreviewSource? = null

    // Steps the receiver holds: neither pushed nor prefetched for it again
    private val receiverHas = mutableSetOf<Int>()
    private val pushes = ArrayDeque<Pair<Int, String>>()

    private var prefetchStartedAt: Long? = null
    private var prefetchMs: Long? = null

    /** The receiver's stream, to prefetch (control channel): a downloaded episode's only. */
    fun onSource(announced: CastPreviewSource) {
        if (!allowed(localFileUri(announced.url) != null)) {
            Log.d(TAG, "No preview for the stream announced: not a downloaded episode, or previews off")
            return
        }
        if (frames.select(announced.sourceId) || source?.sourceId != announced.sourceId) {
            startStream(announced)
        } else {
            // Same stream: the receiver may have lost frames (restarted), they go again
            source = announced
            receiverHas.clear()
            receiverHas += announced.steps
            val center = previewBucketOf(prefetch.positionMs, announced.duration)
            frames.knownPushes(announced, center, receiverHas).forEach(::queuePush)
        }
        scheduleTick(0L)
    }

    /** A scrub's request from the receiver (control channel): before any prefetch. */
    fun onRequest(request: CastPreviewRequest) {
        val localUri = localFileUri(request.url)
        if (!allowed(localUri != null)) {
            Log.d(TAG, "Step ${request.step} refused: preview off for this stream")
            send(CastPreviewProtocol.statusMessage(request, CastPreviewProtocol.STATUS_OFF))
            return
        }
        if (request.sourceId != source?.sourceId) {
            // A stream not announced (yet): prefetched as well from now on
            frames.select(request.sourceId)
            startStream(
                CastPreviewSource(
                    type = CastPreviewProtocol.TYPE_SOURCE,
                    sourceId = request.sourceId,
                    url = request.url,
                    headers = request.headers,
                    duration = request.duration,
                ),
            )
            scheduleTick(0L)
        }
        frames.onRequest(request)?.let(::perform)
        scheduleIdleRelease()
    }

    /** The cast session ended: decoder, pushes and frames released. */
    fun destroy() {
        logStats("Session ended")
        handler.removeCallbacks(tick)
        handler.removeCallbacks(pushNext)
        pushes.clear()
        source = null
        // Cleared first: a decode in flight is dropped, so nothing holds the decoder back
        frames.clear()
        releaseGrabber()
    }

    fun onTrimMemory(level: Int) {
        if (shouldDropPreviewFrames(level)) frames.dropFrames()
    }

    /** The current stream's numbers. */
    fun stats(): Stats {
        val current = source
        return Stats(
            sourceId = current?.sourceId,
            steps = current?.let { previewBucketOf(it.duration - 1, it.duration) + 1 } ?: 0,
            frames = frames.size,
            withoutFrame = frames.unavailableCount,
            prefetchMs = prefetchMs,
            complete = prefetchMs != null,
        )
    }

    private fun startStream(announced: CastPreviewSource) {
        if (source != null) logStats("Stream changed")
        source = announced
        prefetch.reset()
        receiverHas.clear()
        receiverHas += announced.steps
        pushes.clear()
        prefetchStartedAt = null
        prefetchMs = null
    }

    private fun allowed(local: Boolean): Boolean =
        castSeekPreviewAllowed(settingOn = settings.enabled, receiverCapable = receiverCapable(), localFile = local)

    // --- Prefetch -----------------------------------------------------------------------------

    private fun scheduleTick(delayMs: Long) {
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, delayMs)
    }

    private fun onTick() {
        val current = source ?: return
        playback()?.let { prefetch.onStatus(it.state, it.positionMs) } ?: prefetch.onStatus(null, prefetch.positionMs)
        if (frames.isFailed) return
        val allowed = settings.prefetch && allowed(localFileUri(current.url) != null)
        when (val next = nextPrefetch(current, prefetch, frames, allowed, receiverHas)) {
            PrefetchDecision.Complete -> {
                onPrefetchComplete()
                return
            }
            is PrefetchDecision.Start -> {
                if (prefetchStartedAt == null) prefetchStartedAt = clock()
                perform(next.fetch)
            }
            // Ticking on: the playback or the settings may allow it later
            PrefetchDecision.Wait -> Unit
        }
        scheduleTick(TICK_MS)
    }

    private fun onPrefetchComplete() {
        if (prefetchMs != null) return
        prefetchMs = prefetchStartedAt?.let { clock() - it } ?: 0L
        logStats("Prefetch complete")
    }

    private fun logStats(event: String) {
        val stats = stats()
        if (stats.sourceId == null) return
        Log.i(
            TAG,
            "$event: ${stats.frames} frames, ${stats.withoutFrame} without, of ${stats.steps} steps" +
                (stats.prefetchMs?.let { ", prefetched in ${it / 1000} s" } ?: ""),
        )
    }

    // --- Decoding -----------------------------------------------------------------------------

    private fun perform(action: CastPreviewFrames.Action) {
        when (action) {
            is CastPreviewFrames.Action.Fetch -> fetch(action.request)
            is CastPreviewFrames.Action.Reply -> if (action.request.requestId == null) queuePush(action) else reply(action)
        }
    }

    private fun message(action: CastPreviewFrames.Action.Reply): String {
        val frame = action.frame
        return if (action.status == CastPreviewProtocol.STATUS_OK && frame != null) {
            CastPreviewProtocol.frameMessage(action.request, frame)
                ?: CastPreviewProtocol.statusMessage(action.request, CastPreviewProtocol.STATUS_NONE)
        } else {
            CastPreviewProtocol.statusMessage(action.request, action.status)
        }
    }

    private fun reply(action: CastPreviewFrames.Action.Reply) {
        val message = message(action)
        Log.d(TAG, "Step ${action.request.step} -> ${action.status}, ${message.length} chars")
        receiverHas += action.request.step
        send(message)
    }

    private fun fetch(request: CastPreviewRequest) {
        // Allowed for a downloaded episode only
        val localUri = localFileUri(request.url) ?: return
        val previewGrabber = grabber ?: newGrabber().also { grabber = it }
        if (grabberSourceId != request.sourceId) {
            // Read as it is, as the phone player reads a download
            previewGrabber.setSource { DefaultMediaSourceFactory(context).createMediaSource(MediaItem.fromUri(localUri)) }
            grabberSourceId = request.sourceId
        }
        val range = previewBucketRangeMs(request.step, request.duration)
        val targetMs = (range.first + range.last + 1) / 2
        // Any frame of the step will do
        val toleranceMs = (range.last + 1 - range.first) / 2
        val startedAt = SystemClock.elapsedRealtime()
        Log.d(TAG, "Decoding step ${request.step} at $targetMs ms" + if (request.requestId == null) " (prefetch)" else "")
        previewGrabber.grab(targetMs, toleranceMs) { result ->
            handler.post { onGrabbed(request, result, SystemClock.elapsedRealtime() - startedAt) }
        }
        scheduleIdleRelease()
    }

    private fun onGrabbed(request: CastPreviewRequest, result: CastFrameGrabber.Result, elapsedMs: Long) {
        val frame = (result as? CastFrameGrabber.Result.Frame)?.jpeg
        Log.d(
            TAG,
            "Step ${request.step}: ${result::class.simpleName} in $elapsedMs ms" +
                (frame?.let { " (${it.width}x${it.height}, ${it.bytes.size} B)" } ?: ""),
        )
        frames.onFetched(request, frame, sourceFailed = result is CastFrameGrabber.Result.Failed).forEach(::perform)
        if (request.requestId == null) {
            prefetch.onDecodeEnded(CAST_PREFETCH_GAP_LOCAL_MS)
            // The next prefetch right after its gap
            if (source != null) scheduleTick(CAST_PREFETCH_GAP_LOCAL_MS)
        }
        scheduleIdleRelease()
    }

    // --- Pushes -------------------------------------------------------------------------------

    private fun queuePush(action: CastPreviewFrames.Action.Reply) {
        val step = action.request.step
        if (step in receiverHas || pushes.any { it.first == step }) return
        pushes.addLast(step to message(action))
        if (pushes.size == 1) {
            handler.removeCallbacks(pushNext)
            handler.post(pushNext)
        }
    }

    /** One push at a time, [PUSH_INTERVAL_MS] apart: the cast channel also carries the playback's messages. */
    private fun onPush() {
        val (step, message) = pushes.removeFirstOrNull() ?: return
        receiverHas += step
        send(message)
        if (pushes.isNotEmpty()) handler.postDelayed(pushNext, PUSH_INTERVAL_MS)
    }

    // --- Lifecycle ----------------------------------------------------------------------------

    private fun scheduleIdleRelease() {
        handler.removeCallbacks(idleRelease)
        handler.postDelayed(idleRelease, IDLE_RELEASE_MS)
    }

    private fun releaseGrabber() {
        handler.removeCallbacks(idleRelease)
        if (frames.inFlight != null) {
            // Never in the middle of a decode: retried once it's over
            scheduleIdleRelease()
            return
        }
        grabber?.release()
        grabber = null
        grabberSourceId = null
    }

    companion object {
        private const val TAG = "CastSeekPreview"

        /** The decoder (player, GL surface, thread) is freed after this long without decoding. */
        const val IDLE_RELEASE_MS = 30_000L

        /** How often the prefetch looks at the TV's playback. */
        const val TICK_MS = 1_000L

        /** Between two pushed frames. */
        const val PUSH_INTERVAL_MS = 100L
    }
}
