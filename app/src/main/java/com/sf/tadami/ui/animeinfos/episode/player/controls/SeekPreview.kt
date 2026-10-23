package com.sf.tadami.ui.animeinfos.episode.player.controls

import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import com.sf.tadami.ui.animeinfos.episode.player.preview.SeekPreviewPlayer
import com.sf.tadami.ui.utils.formatMinSec
import kotlinx.coroutines.delay

/**
 * Seekbar preview hooks. With them, dragging the seekbar shows [thumbnail] above the thumb and seeks the
 * player only once, through [onCommit], when the gesture ends. Without them the seekbar seeks as it moves.
 */
@Stable
class SeekBarPreview(
    /** Whether a gesture starting now previews (something plays); asked once per gesture. */
    val isAvailable: () -> Boolean,
    val onScrub: (positionMs: Long) -> Unit,
    val onScrubEnd: () -> Unit,
    val onCommit: (positionMs: Long) -> Unit,
    val thumbnail: @Composable (positionMs: Long) -> Unit,
)

/** Aniyomi's seekbar preview size, 16:9. */
private val PreviewWidth = 220.dp
private val PreviewHeight = 124.dp

/** A frame of a step near the one under the finger, shown while that one loads, is dimmed to this. */
private const val NEARBY_FRAME_ALPHA = 0.6f

/** A step loading this long shows a spinner: a downloaded episode's frame usually comes sooner. */
private const val SPINNER_DELAY_MS = 500L

/**
 * The frame of the preview step [positionMs] falls in, with the exact [positionMs] under it. While the step's
 * frame loads, the frame of a step near it stands in, dimmed, or an empty box; a small spinner shows once the
 * same step has loaded for [SPINNER_DELAY_MS]. A streamed episode, or a step that can't have a frame with none
 * near it, shows the time alone.
 */
@OptIn(UnstableApi::class)
@Composable
fun SeekPreviewThumbnail(
    previewPlayer: SeekPreviewPlayer,
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    val shown = remember(previewPlayer.framesVersion, positionMs, durationMs) {
        previewPlayer.frameAt(positionMs, durationMs)
    }
    // The step that has loaded for long: started again on every step, so there's no spinner while the finger
    // moves, only once it rests on a step that is slow to come
    var slowStep by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(shown.bucket, shown.loading) {
        slowStep = null
        if (shown.loading) {
            delay(SPINNER_DELAY_MS)
            slowStep = shown.bucket
        }
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        // Without it, a black card waiting for its first frame is lost on the black letterbox
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val frame = shown.frame
            if (frame != null || shown.loading) {
                Box(
                    modifier = Modifier
                        .size(PreviewWidth, PreviewHeight)
                        .drawWithContent {
                            drawContent()
                            // The frame as drawn (latency logs)
                            previewPlayer.onThumbnailDrawn(shown.bucket, shown.exact)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (frame != null) {
                        val image = remember(frame) { frame.asImageBitmap() }
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            alpha = if (shown.exact) 1f else NEARBY_FRAME_ALPHA,
                            modifier = Modifier.size(PreviewWidth, PreviewHeight),
                        )
                    }
                    if (shown.loading && slowStep == shown.bucket) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                text = positionMs.formatMinSec(),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * The surface the preview frames are decoded into, never seen: 1 px, to be placed under the video (whose
 * surface hides it). It lives as long as the player screen so a fetch ending after the drag still lands.
 */
@OptIn(UnstableApi::class)
@Composable
fun SeekPreviewSurface(previewPlayer: SeekPreviewPlayer, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.size(1.dp),
        factory = { context -> TextureView(context).also(previewPlayer::attachSurface) },
        onRelease = previewPlayer::detachSurface,
    )
}
