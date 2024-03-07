package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import com.sf.tadami.R
import com.sf.tadami.ui.components.material.IconButton
import com.sf.tadami.ui.themes.ComposeRippleTheme

@Composable
fun CenterControls(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    isIdle: Boolean,
    idleLock: Boolean,
    onPauseToggle: () -> Unit,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    hasNext: () -> Boolean,
    hasPrevious: () -> Boolean,
) {
    val hasNextEp = remember(hasNext()) { hasNext() }
    val hasPreviousEp = remember(hasPrevious()) { hasPrevious() }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(32.dp * 2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //replay button
        CompositionLocalProvider(LocalRippleTheme provides ComposeRippleTheme) {

            IconButton(enabled = hasPreviousEp, onClick = onPrevious,size = 40.dp) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_skip_previous),
                    tint = if (hasPreviousEp) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.6f
                    ),
                    contentDescription = "Previous episode"
                )
            }

            // Pause/play toggle button
            IconButton(onClick = onPauseToggle, size = 64.dp) {
                Icon(
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

            // Next episode
            IconButton(enabled = hasNextEp, onClick = onNext, size = 40.dp) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_skip_next),
                    tint = if (hasNextEp) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.6f
                    ),
                    contentDescription = "Next episode"
                )
            }

        }
    }
}