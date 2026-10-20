package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.sf.tadami.ui.animeinfos.episode.player.preview.seekFraction
import com.sf.tadami.ui.animeinfos.episode.player.preview.seekPreviewLeft
import com.sf.tadami.ui.utils.formatMinSec
import com.sf.tadami.ui.utils.padding
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekBar(
    modifier: Modifier = Modifier,
    totalDuration: () -> Long = { 0 },
    currentTime: () -> Long = { 0 },
    bufferPercentage: () -> Int = { 0 },
    onSeekChanged: (timeMs: Float) -> Unit = {},
    onSeekEnd: () -> Unit = {},
    isSeekable: Boolean = true,
    preview: SeekBarPreview? = null,
) {

    val duration = remember(totalDuration()) { totalDuration() }

    val videoTime = remember(currentTime()) { currentTime() }

    val buffer = remember(bufferPercentage()) { bufferPercentage() }
    val interactionSource = remember { MutableInteractionSource() }
    val otherInteractionSource = remember { MutableInteractionSource() }

    // Tells drags from taps; not the thumb's source, so the thumb keeps its look
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val isDragged by sliderInteractionSource.collectIsDraggedAsState()
    // Decided when a gesture starts: true seeks as the slider moves, false previews and seeks on release
    var liveSeek by remember { mutableStateOf<Boolean?>(null) }
    // The position under the thumb while a previewing gesture lasts
    var scrubTime by remember { mutableStateOf<Float?>(null) }
    // The last previewed position, still shown while the preview fades out
    var previewTime by remember { mutableFloatStateOf(0f) }
    var sliderCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val previewing = preview != null && isDragged && liveSeek == false

    // The drag may be recognised after the slider moved: preview where the thumb already is
    LaunchedEffect(previewing) {
        if (previewing) scrubTime?.let { preview.onScrub(it.toLong()) }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // show current video time
        Text(
            modifier = Modifier,
            text = (scrubTime?.toLong() ?: videoTime).formatMinSec(),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelLarge
        )
        Box(modifier = Modifier
            .weight(1f)
            .padding(horizontal = MaterialTheme.padding.medium)) {
            // buffer bar
            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = buffer.toFloat(),
                enabled = false,
                onValueChange = { },
                valueRange = 0f..100f,
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = otherInteractionSource,
                        thumbSize = DpSize(22.dp,22.dp),
                        colors = SliderDefaults.colors(thumbColor = Color.Transparent, disabledThumbColor = Color.Transparent)
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        enabled = false,
                        thumbTrackGapSize = 0.dp,
                        colors = SliderDefaults.colors(
                            disabledActiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.height(12.dp)
                    )
                },
            )

            // seek bar
            Slider(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { sliderCoordinates = it },
                value = scrubTime ?: videoTime.toFloat(),
                enabled = isSeekable,
                onValueChange = { timeMs ->
                    val live = liveSeek ?: (preview?.isAvailable() != true).also { liveSeek = it }
                    if (live) {
                        onSeekChanged(timeMs)
                    } else {
                        scrubTime = timeMs
                        previewTime = timeMs
                        if (isDragged) preview?.onScrub(timeMs.toLong())
                    }
                },
                onValueChangeFinished = {
                    val target = scrubTime
                    if (liveSeek == false) {
                        preview?.onScrubEnd()
                        target?.let { preview?.onCommit(it.toLong()) }
                    }
                    liveSeek = null
                    scrubTime = null
                    onSeekEnd()
                },
                interactionSource = sliderInteractionSource,
                valueRange = 0f..duration.toFloat(),
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        thumbSize = ThumbSize
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        thumbTrackGapSize = 0.dp,
                        colors = SliderDefaults.colors(inactiveTrackColor = Color.Transparent),
                        modifier = Modifier.height(12.dp)
                    )
                }
            )

            if (preview != null) {
                SeekPreviewOverlay(
                    preview = preview,
                    visible = previewing,
                    positionMs = previewTime.toLong(),
                    durationMs = duration,
                    slider = { sliderCoordinates },
                )
            }
        }
        // show total video time
        Text(
            modifier = Modifier,
            text = duration.formatMinSec(),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

private val ThumbSize = DpSize(22.dp, 22.dp)

/** Space between the preview and the slider, and the least between the preview and the screen edges. */
private val PreviewGap = 8.dp
private val PreviewEdgeMargin = 8.dp

/**
 * The preview of [positionMs], drawn above the [slider] and centred on its thumb, within the screen edges.
 * Laid out as 0x0 so the seekbar keeps its size; must be placed at the slider's top start.
 */
@Composable
private fun SeekPreviewOverlay(
    preview: SeekBarPreview,
    visible: Boolean,
    positionMs: Long,
    durationMs: Long,
    slider: () -> LayoutCoordinates?,
) {
    val insets = WindowInsets.safeDrawing
    Box(
        modifier = Modifier.layout { measurable, _ ->
            val placeable = measurable.measure(Constraints())
            val margin = PreviewEdgeMargin.toPx()
            val boundsStart = insets.getLeft(this, layoutDirection) + margin
            val endInset = insets.getRight(this, layoutDirection) + margin
            layout(0, 0) {
                val sliderCoordinates = slider()?.takeIf { it.isAttached } ?: return@layout
                val root = sliderCoordinates.findRootCoordinates()
                val sliderStart = root.localPositionOf(sliderCoordinates, Offset.Zero).x
                val left = seekPreviewLeft(
                    fraction = seekFraction(positionMs, durationMs),
                    sliderStart = sliderStart,
                    sliderWidth = sliderCoordinates.size.width.toFloat(),
                    thumbWidth = ThumbSize.width.toPx(),
                    previewWidth = placeable.width.toFloat(),
                    boundsStart = boundsStart,
                    boundsEnd = root.size.width - endInset,
                )
                placeable.place(
                    x = (left - sliderStart).roundToInt(),
                    y = -placeable.height - PreviewGap.roundToPx(),
                )
            }
        }
    ) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            preview.thumbnail(positionMs)
        }
    }
}
