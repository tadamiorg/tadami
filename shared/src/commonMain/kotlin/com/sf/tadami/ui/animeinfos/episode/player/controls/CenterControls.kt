package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.sf.tadami.shared.resources.Res
import com.sf.tadami.shared.resources.*
import com.sf.tadami.ui.components.material.IconButton
import com.sf.tadami.ui.themes.ComposeRippleConfiguration

@OptIn(ExperimentalMaterial3Api::class)
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
        CompositionLocalProvider(LocalRippleConfiguration provides ComposeRippleConfiguration) {

            IconButton(enabled = hasPreviousEp, onClick = onPrevious,size = 40.dp) {
                Icon(
                    painter = painterResource(Res.drawable.ic_skip_previous),
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
                            painterResource(Res.drawable.ic_pause)
                        }

                        isIdle && !idleLock -> {
                            painterResource(Res.drawable.ic_replay)
                        }

                        else -> {
                            painterResource(Res.drawable.ic_play)
                        }
                    },
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = "Play/Pause"
                )
            }

            // Next episode
            IconButton(enabled = hasNextEp, onClick = onNext, size = 40.dp) {
                Icon(
                    painter = painterResource(Res.drawable.ic_skip_next),
                    tint = if (hasNextEp) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(
                        alpha = 0.6f
                    ),
                    contentDescription = "Next episode"
                )
            }

        }
    }
}