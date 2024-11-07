package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.sf.tadami.ui.utils.formatMinSec
import com.sf.tadami.ui.utils.padding

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
) {

    val duration = remember(totalDuration()) { totalDuration() }

    val videoTime = remember(currentTime()) { currentTime() }

    val buffer = remember(bufferPercentage()) { bufferPercentage() }
    val interactionSource = remember { MutableInteractionSource() }
    val otherInteractionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // show current video time
        Text(
            modifier = Modifier,
            text = videoTime.formatMinSec(),
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
                modifier = Modifier.fillMaxWidth(),
                value = videoTime.toFloat(),
                enabled = isSeekable,
                onValueChange = onSeekChanged,
                onValueChangeFinished = onSeekEnd,
                valueRange = 0f..duration.toFloat(),
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        thumbSize = DpSize(22.dp,22.dp)
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