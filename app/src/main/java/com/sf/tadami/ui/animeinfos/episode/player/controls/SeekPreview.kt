package com.sf.tadami.ui.animeinfos.episode.player.controls

import android.view.TextureView
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    /** Whether a gesture starting now previews (the setting, the network); asked once per gesture. */
    val isAvailable: () -> Boolean,
    val onScrub: (positionMs: Long) -> Unit,
    val onScrubEnd: () -> Unit,
    val onCommit: (positionMs: Long) -> Unit,
    val thumbnail: @Composable (positionMs: Long) -> Unit,
)

/** Aniyomi's seekbar preview size, 16:9. */
private val PreviewWidth = 220.dp
private val PreviewHeight = 124.dp

/** The frame at [positionMs] with that time under it; a spinner until the first frame arrives. */
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
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!previewPlayer.failed) {
                Box(modifier = Modifier.size(PreviewWidth, PreviewHeight), contentAlignment = Alignment.Center) {
                    AndroidView(
                        modifier = Modifier.aspectRatio(previewPlayer.videoAspectRatio),
                        factory = { context -> TextureView(context).also(previewPlayer::attachSurface) },
                        onRelease = previewPlayer::detachSurface,
                    )
                    if (!previewPlayer.hasFrame) {
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
