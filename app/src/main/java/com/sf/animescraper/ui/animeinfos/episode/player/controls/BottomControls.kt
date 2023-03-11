package com.sf.animescraper.ui.animeinfos.episode.player.controls

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sf.animescraper.R
import com.sf.animescraper.ui.utils.formatMinSec

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun BottomControls(
    modifier: Modifier = Modifier,
    totalDuration: () -> Long = { 0 },
    currentTime: () -> Long = { 0 },
    bufferPercentage: () -> Int = { 0 },
    onSeekChanged: (timeMs: Float) -> Unit = {},
    onSkipOp: () -> Unit = {},
    onSettings: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
) {

    val duration = remember(totalDuration()) { totalDuration() }

    val videoTime = remember(currentTime()) { currentTime() }

    val buffer = remember(bufferPercentage()) { bufferPercentage() }
    val interactionSource = remember { MutableInteractionSource() }


    Column(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {

            Row(modifier = Modifier.align(Alignment.CenterStart),verticalAlignment = Alignment.CenterVertically) {

                // Video Settings Button
                IconButton(modifier = Modifier, onClick = onSettings) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video_settings),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "Settings"
                    )
                }

                // Ignore Opening Button
                TextButton(onClick = onSkipOp) {
                    Text(
                        text = stringResource(id = R.string.player_screen_controls_forward_85),
                        softWrap = false,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                }

            }


            Row(modifier = Modifier.align(Alignment.Center),verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(40.dp)) {

                // Previous episode

                IconButton(onClick = onPrevious) {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(id = R.drawable.ic_skip_previous),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "Previous episode"
                    )
                }

                // Next episode
                IconButton(onClick = onNext) {
                    Icon(
                        modifier = Modifier.size(40.dp),
                        painter = painterResource(id = R.drawable.ic_skip_next),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "Next episode"
                    )
                }
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // show current video time
            Text(
                modifier = Modifier,
                text = videoTime.formatMinSec(),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelMedium
            )
            Box(modifier = Modifier.weight(1f)) {
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
                    onValueChange = onSeekChanged,
                    valueRange = 0f..duration.toFloat(),
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = interactionSource,
                            thumbSize = DpSize(15.dp, 15.dp),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    },
                    colors = SliderDefaults.colors(
                        inactiveTrackColor = Color.Transparent
                    )
                )
            }
            // show total video time
            Text(
                modifier = Modifier,
                text = duration.formatMinSec(),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelMedium
            )

        }
    }


}