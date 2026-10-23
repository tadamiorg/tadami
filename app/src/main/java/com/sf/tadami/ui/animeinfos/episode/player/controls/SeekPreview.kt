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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
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

/**
 * The frame of the preview step [positionMs] falls in, with the exact [positionMs] under it. While the
 * step's frame loads, the last frame shown stays with a spinner over it, or the spinner alone at first.
 * A streamed episode, or one the preview player failed on, shows the time alone.
 */
@OptIn(UnstableApi::class)
@Composable
fun SeekPreviewThumbnail(
    previewPlayer: SeekPreviewPlayer,
    positionMs: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        // Without it, a black card waiting for its first frame is lost on the black letterbox
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val frame = previewPlayer.frame
            if (previewPlayer.hasSource && (!previewPlayer.failed || frame != null)) {
                Box(
                    modifier = Modifier
                        .size(PreviewWidth, PreviewHeight)
                        .drawWithContent {
                            drawContent()
                            // Read at draw time: the frame and its state as drawn (latency logs)
                            previewPlayer.onThumbnailDrawn(exact = previewPlayer.frame != null && !previewPlayer.frameLoading)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (frame != null) {
                        val image = remember(frame) { frame.asImageBitmap() }
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(PreviewWidth, PreviewHeight),
                        )
                    }
                    if (previewPlayer.frameLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
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
