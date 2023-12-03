package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.R
import com.sf.tadami.ui.themes.ComposeRippleTheme

@Composable
fun CenterControls(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    onReplay: () -> Unit,
    isIdle : Boolean,
    idleLock : Boolean,
    onPauseToggle: () -> Unit,
    onForward : () -> Unit,
) {
    val size = 50.dp

    Row(
        modifier = modifier.padding(top = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(size*2, Alignment.CenterHorizontally)
    ) {
        //replay button
        CompositionLocalProvider(LocalRippleTheme provides ComposeRippleTheme) {

            IconButton(modifier = Modifier.size(size), enabled = !isIdle,onClick = onReplay) {
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
                        isPlaying -> {
                            painterResource(id = R.drawable.ic_pause)
                        }
                        isIdle && !idleLock -> {
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
            IconButton(modifier = Modifier.size(size), enabled = !isIdle,onClick = onForward) {
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