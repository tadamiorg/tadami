package com.sf.tadami.ui.animeinfos.episode.player.preview

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.video.MediaCodecVideoRenderer

/**
 * The preview player's tracks: the lowest-bitrate video only, no audio nor subtitles, so a scrub
 * fetches the smallest segments and decodes the smallest frames.
 */
fun previewTrackSelectionParameters(base: TrackSelectionParameters): TrackSelectionParameters {
    return base.buildUpon()
        .setForceLowestBitrate(true)
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        .build()
}

/** A single video renderer for the preview players: no audio, text nor metadata renderer is even created. */
@OptIn(UnstableApi::class)
fun previewRenderersFactory(context: Context) = RenderersFactory { handler, videoListener, _, _, _ ->
    arrayOf(
        MediaCodecVideoRenderer.Builder(context)
            .setEventHandler(handler)
            .setEventListener(videoListener)
            .setEnableDecoderFallback(true)
            .build()
    )
}

/** The preview players' buffering: about one segment around the target, nothing more, nothing kept behind. */
@OptIn(UnstableApi::class)
fun previewLoadControl(): DefaultLoadControl = DefaultLoadControl.Builder()
    .setBufferDurationsMs(PREVIEW_MIN_BUFFER_MS, PREVIEW_MAX_BUFFER_MS, PREVIEW_MIN_BUFFER_MS, PREVIEW_MIN_BUFFER_MS)
    .setTargetBufferBytes(PREVIEW_TARGET_BUFFER_BYTES)
    .setPrioritizeTimeOverSizeThresholds(false)
    .setBackBuffer(0, false)
    .build()

private const val PREVIEW_MIN_BUFFER_MS = 500
private const val PREVIEW_MAX_BUFFER_MS = 1_000
private const val PREVIEW_TARGET_BUFFER_BYTES = 4 * 1024 * 1024

/**
 * [argb] as a preview frame: RGB_565, half the memory, dithered so gradients don't band. [argb] is
 * recycled.
 */
fun toPreviewFrame(argb: Bitmap): Bitmap {
    val frame = Bitmap.createBitmap(argb.width, argb.height, Bitmap.Config.RGB_565)
    Canvas(frame).drawBitmap(argb, 0f, 0f, Paint(Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG))
    argb.recycle()
    return frame
}

/**
 * Whether a memory trim drops the preview frames: on memory pressure, or once the app is a background
 * process (which leaving for the home screen may make it). Hiding the UI alone (pause, PiP) keeps them.
 */
@Suppress("DEPRECATION")
fun shouldDropPreviewFrames(trimLevel: Int): Boolean {
    return trimLevel == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
        trimLevel == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
        trimLevel >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
}

/** Whether a trim at [trimLevel] comes from memory pressure, not from the app going to the background. */
@Suppress("DEPRECATION")
fun isMemoryPressureTrim(trimLevel: Int): Boolean =
    trimLevel == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW || trimLevel == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
