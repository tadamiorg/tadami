package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.tv.material3.MaterialTheme
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
        Box(modifier = Modifier.weight(1f).padding(horizontal = MaterialTheme.padding.medium)) {
            // buffer bar
            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = buffer.toFloat(),
                enabled = false,
                onValueChange = { },
                valueRange = 0f..100f,
                colors =
                SliderDefaults.colors(
                    disabledThumbColor = Color.Transparent,
                    disabledActiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
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
                        thumbSize = DpSize(15.dp, 15.dp),
                        modifier = Modifier.align(Alignment.Center)
                    )
                },
                colors = SliderDefaults.colors(
                    inactiveTrackColor = Color.Transparent,
                    disabledActiveTrackColor = MaterialTheme.colorScheme.primary
                )
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