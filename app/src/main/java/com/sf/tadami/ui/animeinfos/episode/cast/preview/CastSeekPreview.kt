package com.sf.tadami.ui.animeinfos.episode.cast.preview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import com.sf.tadami.network.player.HlsPlaylistFixingDataSource
import com.sf.tadami.network.player.PlayerNetworkHelper
import com.sf.tadami.ui.animeinfos.episode.player.preview.previewBucketRangeMs
import com.sf.tadami.ui.animeinfos.episode.player.preview.shouldDropPreviewFrames

/**
 * The cast session's seekbar preview: answers the web receiver's `previewRequest`s with frames of the
 * stream the TV plays, decoded on the phone ([CastFrameGrabber]) and cached for the session
 * ([CastPreviewFrames]). Owned by the cast service, so it works with the player screen closed.
 *
 * The stream is the one the request names — the URL and headers the phone sent with the load, read
 * as the phone player reads them (OkHttp, the player cache, HLS segment IV fix) — or, behind a proxy
 * `/local-` URL, the downloaded file itself ([localFileUri]). The decoder is released after
 * [IDLE_RELEASE_MS] without requests, and with [destroy] at the end of the session. Main thread only.
 */
@UnstableApi
class CastSeekPreview(
    private val context: Context,
    private val playerNetworkHelper: PlayerNetworkHelper,
    /** Sends a reply on the control channel. */
    private val send: (String) -> Unit,
    /** The downloaded file a proxy `/local-` URL serves, null for any other URL. */
    private val localFileUri: (String) -> String?,
    /** The receiver announced the seekPreview capability. */
    private val receiverCapable: () -> Boolean,
    private val isOnWifi: () -> Boolean,
    private val newGrabber: () -> CastFrameGrabber = { CastFrameGrabber(context) },
) {
    /** The preferences that apply, kept up to date by the owner. */
    data class Settings(val enabled: Boolean = false, val onlyOnWifi: Boolean = false, val userAgent: String = "")

    @Volatile
    var settings = Settings()

    private val frames = CastPreviewFrames()
    private val handler = Handler(Looper.getMainLooper())
    private val idleRelease = Runnable { releaseGrabber() }
    private var grabber: CastFrameGrabber? = null
    private var grabberSourceId: String? = null

    /** A request from the receiver (control channel). */
    fun onRequest(request: CastPreviewRequest) {
        val localUri = localFileUri(request.url)
        val current = settings
        val allowed = castSeekPreviewAllowed(
            settingOn = current.enabled,
            onlyOnWifi = current.onlyOnWifi,
            onWifi = isOnWifi(),
            receiverCapable = receiverCapable(),
            localFile = localUri != null,
        )
        if (!allowed) {
            Log.d(TAG, "Step ${request.step} refused: preview off for this stream")
            send(CastPreviewProtocol.statusMessage(request, CastPreviewProtocol.STATUS_OFF))
            return
        }
        frames.onRequest(request)?.let { perform(it, localUri) }
        scheduleIdleRelease()
    }

    /** The cast session ended: decoder and frames released. */
    fun destroy() {
        // Cleared first: a decode in flight is dropped, so nothing holds the decoder back
        frames.clear()
        releaseGrabber()
    }

    fun onTrimMemory(level: Int) {
        if (shouldDropPreviewFrames(level)) frames.dropFrames()
    }

    private fun perform(action: CastPreviewFrames.Action, localUri: String?) {
        when (action) {
            is CastPreviewFrames.Action.Fetch -> fetch(action.request, localUri)
            is CastPreviewFrames.Action.Reply -> reply(action)
        }
    }

    private fun reply(action: CastPreviewFrames.Action.Reply) {
        val frame = action.frame
        val message = if (action.status == CastPreviewProtocol.STATUS_OK && frame != null) {
            CastPreviewProtocol.frameMessage(action.request, frame)
                ?: CastPreviewProtocol.statusMessage(action.request, CastPreviewProtocol.STATUS_NONE)
        } else {
            CastPreviewProtocol.statusMessage(action.request, action.status)
        }
        Log.d(TAG, "Step ${action.request.step} -> ${action.status}, ${message.length} chars")
        send(message)
    }

    private fun fetch(request: CastPreviewRequest, localUri: String?) {
        val previewGrabber = grabber ?: newGrabber().also { grabber = it }
        if (grabberSourceId != request.sourceId) {
            previewGrabber.setSource(mediaSourceFactory(request, localUri))
            grabberSourceId = request.sourceId
        }
        val range = previewBucketRangeMs(request.step, request.duration)
        val targetMs = (range.first + range.last + 1) / 2
        // Any frame of the step will do
        val toleranceMs = (range.last + 1 - range.first) / 2
        val startedAt = SystemClock.elapsedRealtime()
        Log.d(TAG, "Decoding step ${request.step} at $targetMs ms")
        previewGrabber.grab(targetMs, toleranceMs) { result ->
            handler.post { onGrabbed(request, result, SystemClock.elapsedRealtime() - startedAt) }
        }
    }

    private fun onGrabbed(request: CastPreviewRequest, result: CastFrameGrabber.Result, elapsedMs: Long) {
        val frame = (result as? CastFrameGrabber.Result.Frame)?.jpeg
        Log.d(
            TAG,
            "Step ${request.step}: ${result::class.simpleName} in $elapsedMs ms" +
                (frame?.let { " (${it.width}x${it.height}, ${it.bytes.size} B)" } ?: ""),
        )
        frames.onFetched(request, frame, sourceFailed = result is CastFrameGrabber.Result.Failed)
            .forEach { perform(it, localFileUri(it.requestOf().url)) }
        if (frames.inFlight == null) {
            Log.d(TAG, "Cache: ${frames.size} frames, ${frames.bytes / 1024} KiB")
        }
        scheduleIdleRelease()
    }

    private fun CastPreviewFrames.Action.requestOf() = when (this) {
        is CastPreviewFrames.Action.Fetch -> request
        is CastPreviewFrames.Action.Reply -> request
    }

    /** Reads the stream as the phone player does: its headers (or the User-Agent setting), cache and HLS fix. */
    private fun mediaSourceFactory(request: CastPreviewRequest, localUri: String?): () -> MediaSource {
        if (localUri != null) {
            // Downloaded files: read as they are, no headers
            return { DefaultMediaSourceFactory(context).createMediaSource(MediaItem.fromUri(localUri)) }
        }
        val headers = request.headers ?: mapOf("User-Agent" to settings.userAgent)
        return {
            val upstream = DefaultDataSource.Factory(context, OkHttpDataSource.Factory(playerNetworkHelper.okHttpClient))
            val cached = CacheDataSource.Factory()
                .setCache(playerNetworkHelper.cache)
                .setUpstreamDataSourceFactory(upstream)
            val resolving = ResolvingDataSource.Factory(HlsPlaylistFixingDataSource.Factory(cached)) { dataSpec ->
                dataSpec.withRequestHeaders(headers)
            }
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(resolving)
                .createMediaSource(MediaItem.fromUri(request.url))
        }
    }

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

        /** The decoder (player, GL surface, thread) is freed after this long without a request. */
        const val IDLE_RELEASE_MS = 30_000L
    }
}
