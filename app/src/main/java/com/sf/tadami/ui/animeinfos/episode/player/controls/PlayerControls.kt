package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sf.tadami.ui.utils.padding

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    isVisible: () -> Boolean,
    isPlaying: Boolean,
    title: () -> String,
    episode: String,
    onReplay: () -> Unit,
    onSkipOp: () -> Unit,
    onPauseToggle: () -> Unit,
    isIdle: Boolean = false,
    idleLock: Boolean = false,
    totalDuration: () -> Long,
    currentTime: () -> Long,
    bufferedPercentage: () -> Int,
    onSeekChanged: (timeMs: Float) -> Unit,
    onSeekEnd: () -> Unit = {},
    onStreamSettings: () -> Unit,
    onPlayerSettings: () -> Unit,
    onEpisodesClicked: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    hasNext: () -> Boolean,
    hasPrevious: () -> Boolean,
    onForward: () -> Unit,
    onTapYoutube : () -> Unit,
    playerSeekValue : () -> Long,
    onPipClicked : () -> Unit,
    videoSettingsEnabled: Boolean = false,
    lockedControls : Boolean
) {
    val visible = remember(isVisible()) { isVisible() }
    var isSeeking by remember { mutableStateOf(false) }
    val overlayAlpha by animateFloatAsState(targetValue = if((visible || isSeeking) && !lockedControls) 0.6f else 0f, label = "")


    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background.copy(alpha = overlayAlpha))) {
        GetYoutubeGesture(
            onSeekForward = onForward,
            onSeekBackward = onReplay,
            onSimpleTap = onTapYoutube,
            playerSeekValue = playerSeekValue,
            lockedControls = lockedControls,
            onSeekingChange = {
                isSeeking = it
            }
        )
        AnimatedVisibility(
            modifier = modifier,
            visible = visible && !isSeeking && !lockedControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.background(Color.Transparent)) {
                // video title
                TopControl(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = MaterialTheme.padding.medium)
                        .fillMaxWidth(),
                    title = title,
                    episode = episode,
                    onBackClicked = onBack
                )

                // center player controls
                CenterControls(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    isPlaying = isPlaying,
                    onPauseToggle = onPauseToggle,
                    isIdle = isIdle,
                    idleLock = idleLock,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    hasNext = hasNext,
                    hasPrevious = hasPrevious,
                )

            }
        }
        Box(modifier = modifier) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = MaterialTheme.padding.medium)
                    .fillMaxWidth(),
            ) {
                // bottom controls
                AnimatedVisibility(
                    visible = visible && !isSeeking && !lockedControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    BottomControls(
                        modifier = Modifier.fillMaxWidth(),
                        onSkipOp = onSkipOp,
                        onStreamSettings = onStreamSettings,
                        isSeekable = !isIdle,
                        videoSettingsEnabled = videoSettingsEnabled,
                        onPlayerSettings = onPlayerSettings,
                        onEpisodesClicked = onEpisodesClicked,
                        onPipClicked = onPipClicked
                    )
                }
                AnimatedVisibility(
                    visible = (visible || isSeeking) && !lockedControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SeekBar(
                        modifier = Modifier.padding(horizontal = MaterialTheme.padding.small),
                        totalDuration = totalDuration,
                        currentTime = currentTime,
                        bufferPercentage = bufferedPercentage,
                        onSeekChanged = onSeekChanged,
                        onSeekEnd = onSeekEnd,
                        isSeekable = !isIdle,
                    )
                }
            }
        }
    }


}