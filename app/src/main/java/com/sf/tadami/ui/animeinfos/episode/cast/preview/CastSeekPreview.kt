package com.sf.tadami.ui.animeinfos.episode.cast.preview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.sf.tadami.data.download.EpisodePreviews
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewPrefetch
import com.sf.tadami.ui.animeinfos.episode.player.preview.PreviewSidecar
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketOf
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketRangeMs
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketSizeMs
import com.sf.tadami.ui.animeinfos.episode.player.preview.shouldDropPreviewFrames
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The cast session's seekbar preview of a downloaded episode: frames of the file the phone serves to the TV,
 * decoded on the phone ([CastFrameGrabber]), cached for the session ([CastPreviewFrames]) and sent to the web
 * receiver. Owned by the cast service, so it works with the player screen closed. A streamed episode has none,
 * as in the phone player: the phone doesn't announce previews for it, refuses its requests ("off") and never
 * reads it, so its scrubs show the time alone and cost no data.
 *
 * - Frames kept next to the episode ([localPreviews], made after its download): as soon as the receiver
 *   announces its stream ([onSource]), every step's frame is pushed to it, the JPEG as stored, nearest the
 *   playback position first, as fast as the cast channel takes them ([PushPacer]): nothing is decoded.
 * - Otherwise, prefetch: once the TV has played steadily for a few seconds, every step is decoded in the
 *   background, nearest the playback position first ([PreviewPrefetch]), and pushed the same way; once every
 *   step is done, the frames are kept next to the episode for the next time.
 * - Scrub: a step the receiver lacks is asked for ([onRequest]), answered from the cache at once, else decoded
 *   before any prefetch; one not there within [CAST_PREVIEW_REPLY_MS] is answered "none" (the receiver shows the
 *   time alone), its frame pushed once decoded ([PendingReplies]).
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
    /** The main thread's: the frames kept next to the episode are read and written off it. */
    private val scope: CoroutineScope,
    /** The frames kept next to the downloaded episode a proxy `/local-` URL serves, null for any other URL. */
    private val localPreviews: (String) -> EpisodePreviews? = { null },
    /** Sends a pushed frame on the control channel, [onSent] (any thread) once the channel took it. */
    private val sendPush: (message: String, onSent: () -> Unit) -> Unit = { message, onSent ->
        send(message)
        onSent()
    },
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
    // Encoded as each goes, not all at once on the main thread
    private val pushes = ArrayDeque<CastPreviewFrames.Action.Reply>()

    private var prefetchStartedAt: Long? = null
    private var prefetchMs: Long? = null

    // The frames kept next to the downloaded episode: read as its stream starts, written once all are decoded
    private var previews: EpisodePreviews? = null
    private var sidecarJob: Job? = null
    private var sidecarLoading = false
    private var sidecarFound = false
    private var sidecarUsed = false
    private var sidecarSaved = false

    // A request waiting for the frames kept next to the episode, and the time of each decoded frame
    private var held: CastPreviewRequest? = null
    private val frameTimesMs = mutableMapOf<Int, Long>()
    private val replies = PendingReplies()

    private val pacer = PushPacer(PUSH_GAP_MS, PUSH_SENT_TIMEOUT_MS)
    private var pushToken = 0
    private var pushBurstStartedAt: Long? = null
    private var pushBurstCount = 0

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
        if (sidecarLoading) {
            // Answered from the frames kept next to the episode once they are read
            held = request
            awaitReply(request)
        } else {
            when (val action = frames.onRequest(request)) {
                is CastPreviewFrames.Action.Reply -> perform(action)
                is CastPreviewFrames.Action.Fetch -> {
                    awaitReply(request)
                    perform(action)
                }
                // Behind the decode in flight
                null -> awaitReply(request)
            }
        }
        scheduleIdleRelease()
    }

    /** The cast session ended: decoder, pushes and frames released. */
    fun destroy() {
        logStats("Session ended")
        handler.removeCallbacks(tick)
        handler.removeCallbacks(pushNext)
        pushes.clear()
        sidecarJob?.cancel()
        replies.clear()
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
        held = null
        frameTimesMs.clear()
        replies.clear()
        pacer.reset()
        pushToken++
        pushBurstStartedAt = null
        handler.removeCallbacks(pushNext)
        sidecarJob?.cancel()
        sidecarLoading = false
        sidecarFound = false
        sidecarUsed = false
        sidecarSaved = false
        previews = localPreviews(announced.url)
        loadSidecar(announced)
    }

    /** Reads the frames kept next to the episode off the main thread, then pushes them all to the receiver. */
    private fun loadSidecar(stream: CastPreviewSource) {
        val target = previews ?: return
        sidecarLoading = true
        val startedAt = clock()
        sidecarJob = scope.launch {
            val sidecar = withContext(Dispatchers.IO) { target.read() }
            if (source?.sourceId != stream.sourceId) return@launch
            sidecarLoading = false
            sidecarFound = sidecar != null
            val known = sidecar?.let { castSidecarFrames(it, stream.duration) }
            if (known == null) {
                Log.i(
                    TAG,
                    if (sidecar == null) {
                        "No preview frames kept next to the episode: decoded on the phone"
                    } else {
                        "Preview frames kept for ${sidecar.durationMs} ms, the TV plays ${stream.duration} ms: decoded on the phone"
                    },
                )
            } else {
                frames.fill(stream.sourceId, known)
                sidecarUsed = true
                Log.i(
                    TAG,
                    "Loaded ${known.frames.size} preview frames kept next to the episode, ${known.none.size} steps " +
                        "without, in ${clock() - startedAt} ms",
                )
                if (settings.prefetch) {
                    val center = previewBucketOf(playback()?.positionMs ?: prefetch.positionMs, stream.duration)
                    frames.knownPushes(stream, center, receiverHas).forEach(::queuePush)
                }
            }
            held?.let { request ->
                held = null
                frames.onRequest(request)?.let(::perform)
            }
            scheduleTick(0L)
        }
    }

    /** [request] isn't answered at once: "none" in [CAST_PREVIEW_REPLY_MS] if its frame isn't there by then. */
    private fun awaitReply(request: CastPreviewRequest) {
        val id = request.requestId ?: return
        replies.waiting(request)
        handler.postDelayed({
            replies.answerEarly(id)?.let { early ->
                Log.d(TAG, "Step ${early.step}: no frame yet, answered none at once")
                send(CastPreviewProtocol.statusMessage(early, CastPreviewProtocol.STATUS_NONE))
            }
        }, CAST_PREVIEW_REPLY_MS)
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
        // The frames kept next to the episode come first: nothing is decoded before they are known
        if (sidecarLoading) {
            scheduleTick(TICK_MS)
            return
        }
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
        saveSidecar()
    }

    /**
     * Every step decoded on the phone: the frames are kept next to the episode, as the JPEGs pushed, so the next
     * cast (or the phone player) has them at once. Not over frames kept already, even another duration's.
     */
    private fun saveSidecar() {
        val stream = source ?: return
        val target = previews ?: return
        val last = previewBucketOf(stream.duration - 1, stream.duration)
        if (sidecarFound || sidecarSaved || !settings.prefetch || (0..last).any { !frames.isDone(it) }) return
        val first = (0..last).firstNotNullOfOrNull(frames::frameOf) ?: return
        sidecarSaved = true
        // A step without a frame (none in time) is left out, not kept as one without: it may come next time
        val entries = (0..last).mapNotNull { step ->
            frames.frameOf(step)?.let { PreviewSidecar.Entry(step, frameTimesMs[step] ?: -1L, it.bytes) }
        }
        scope.launch(Dispatchers.IO) {
            val sidecar = PreviewSidecar(
                videoLength = target.video.length(),
                durationMs = stream.duration,
                stepMs = previewBucketSizeMs(stream.duration),
                frameWidth = first.width,
                frameHeight = first.height,
                entries = entries,
            )
            target.write(context, sidecar)
        }
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
        when (replies.onReply(action.request, action.status)) {
            PendingReplies.Answer.REPLY -> {
                val message = message(action)
                Log.d(TAG, "Step ${action.request.step} -> ${action.status}, ${message.length} chars")
                receiverHas += action.request.step
                send(message)
            }
            // Answered "none" already: the frame comes as a push
            PendingReplies.Answer.PUSH ->
                queuePush(CastPreviewFrames.Action.Reply(action.request.copy(requestId = null), action.status, action.frame))
            PendingReplies.Answer.DROP -> Unit
        }
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
        (result as? CastFrameGrabber.Result.Frame)?.let { frameTimesMs[request.step] = it.timeMs }
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
        if (step in receiverHas || pushes.any { it.request.step == step }) return
        pushes.addLast(action)
        if (pushes.size == 1) {
            handler.removeCallbacks(pushNext)
            handler.postDelayed(pushNext, pacer.delayAt(clock()))
        }
    }

    /**
     * One push at a time, the next [PUSH_GAP_MS] after the channel sent the previous one: as fast as it takes them,
     * never piling up in front of the playback's messages.
     */
    private fun onPush() {
        handler.removeCallbacks(pushNext)
        val wait = pacer.delayAt(clock())
        if (wait > 0L) {
            handler.postDelayed(pushNext, wait)
            return
        }
        val action = pushes.removeFirstOrNull() ?: return
        val message = message(action)
        receiverHas += action.request.step
        if (pushBurstStartedAt == null) {
            pushBurstStartedAt = clock()
            pushBurstCount = 0
        }
        pushBurstCount++
        pacer.onSending(clock())
        val token = ++pushToken
        sendPush(message) { handler.post { onPushSent(token) } }
        // Should the channel never say it sent it
        handler.postDelayed(pushNext, PUSH_SENT_TIMEOUT_MS)
    }

    private fun onPushSent(token: Int) {
        if (token != pushToken) return
        pacer.onSent(clock())
        handler.removeCallbacks(pushNext)
        if (pushes.isNotEmpty()) {
            handler.postDelayed(pushNext, PUSH_GAP_MS)
            return
        }
        // A burst only: a frame decoded is pushed alone, logged with its decode
        pushBurstStartedAt?.takeIf { pushBurstCount > 1 }?.let { Log.i(TAG, "Pushed $pushBurstCount frames to the TV in ${clock() - it} ms") }
        pushBurstStartedAt = null
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

        /** After the channel sent a pushed frame, before the next one: room for the playback's messages. */
        const val PUSH_GAP_MS = 10L

        /** A pushed frame the channel never confirmed: the next one goes after this long anyway. */
        const val PUSH_SENT_TIMEOUT_MS = 500L
    }
}
