package com.sf.animescraper.ui.animeinfos.episode.player.controls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.sf.animescraper.R
import com.sf.animescraper.ui.themes.ComposeRippleTheme

@Composable
fun CenterControls(
    modifier: Modifier = Modifier,
    isPlaying: () -> Boolean,
    playbackState: () -> Int,
    onReplay: () -> Unit,
    onPauseToggle: () -> Unit,
    onForward : () -> Unit,
) {
    val isVideoPlaying = remember(isPlaying()) { isPlaying() }
    val playerState = remember(playbackState()) { playbackState() }
    val size = 50.dp

    Row(
        modifier = modifier.padding(top = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(size*2, Alignment.CenterHorizontally)
    ) {
        //replay button
        CompositionLocalProvider(LocalRippleTheme provides ComposeRippleTheme) {

            IconButton(modifier = Modifier.size(size), onClick = onReplay) {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = R.drawable.ic_replay_10),
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = "Replay 5 seconds",
                )
            }

            //pause/play toggle button
            IconButton(modifier = Modifier.size(size), onClick = onPauseToggle) {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = when {
                        isVideoPlaying -> {
                            painterResource(id = R.drawable.ic_pause)
                        }
                        isVideoPlaying.not() && playerState == STATE_ENDED -> {
                            painterResource(id = R.drawable.ic_replay)
                        }
                        else -> {
                            painterResource(id = R.drawable.ic_play)
                        }
                    },
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = "Play/Pause"
                )
            }

            // Forward button
            IconButton(modifier = Modifier.size(size), onClick = onForward) {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = R.drawable.ic_forward_10),
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = "Forward 10 seconds"
                )
            }
        }
    }
}