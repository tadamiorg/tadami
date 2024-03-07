package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import com.sf.tadami.R
import com.sf.tadami.ui.components.material.IconButton

@Composable
fun BottomControls(
    modifier: Modifier = Modifier,
    onSkipOp: () -> Unit = {},
    onStreamSettings: () -> Unit = {},
    onPlayerSettings: () -> Unit = {},
    onEpisodesClicked: () -> Unit = {},
    isSeekable: Boolean = true,
    videoSettingsEnabled: Boolean = false,
    onPipClicked : (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {

        Row(
            modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row {
                IconButton(
                    onClick = onPlayerSettings,
                    enabled = true,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "Settings"
                    )
                }
                // Video Settings Button
                IconButton(
                    onClick = onStreamSettings,
                    enabled = videoSettingsEnabled,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video_settings),
                        tint = if (videoSettingsEnabled) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onBackground.copy(
                            alpha = 0.5f
                        ),
                        contentDescription = "Video Settings"
                    )
                }

                IconButton(
                    onClick = onEpisodesClicked,
                    enabled = true,
                ) {
                    Icon(
                        modifier = Modifier.graphicsLayer {
                            rotationZ = -90f
                        },
                        painter = painterResource(id = R.drawable.ic_episodes),
                        tint = MaterialTheme.colorScheme.onBackground,
                        contentDescription = "Episodes"
                    )
                }

            }

            Row {
                // Ignore Opening Button
                Button(onClick = onSkipOp, enabled = isSeekable) {
                    Text(
                        text = stringResource(id = R.string.player_screen_controls_forward_85),
                        softWrap = false,
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                }

                onPipClicked?.let {
                    IconButton(
                        onClick = onPipClicked,
                        enabled = true,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pip),
                            tint = MaterialTheme.colorScheme.onBackground,
                            contentDescription = "Picture in picture"
                        )
                    }
                }
            }
        }
    }
}