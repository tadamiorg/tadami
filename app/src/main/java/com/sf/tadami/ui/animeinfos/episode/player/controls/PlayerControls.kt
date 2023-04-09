package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun PlayerControls(
    modifier: Modifier = Modifier,
    isVisible: () -> Boolean,
    isPlaying: () -> Boolean,
    title: () -> String,
    episode: String,
    onReplay: () -> Unit,
    onSkipOp: () -> Unit,
    onPauseToggle: () -> Unit,
    totalDuration: () -> Long,
    currentTime: () -> Long,
    bufferedPercentage: () -> Int,
    playbackState: () -> Int,
    onSeekChanged: (timeMs: Float) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    onCast: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    hasNext : () -> Boolean,
    hasPrevious : () -> Boolean,
    onForward: () -> Unit,
    videoSettingsEnabled : Boolean = false
) {
    val visible = remember(isVisible()) { isVisible() }

    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))) {
            // video title
            TopControl(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(),
                title = title,
                episode = episode,
                onBackClicked = onBack,
                onCastClicked = onCast
            )

            // center player controls
            CenterControls(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                isPlaying = isPlaying,
                playbackState = playbackState,
                onReplay = onReplay,
                onPauseToggle = onPauseToggle,
                onForward = onForward,

            )

            // bottom controls
            BottomControls(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                totalDuration = totalDuration,
                currentTime = currentTime,
                bufferPercentage = bufferedPercentage,
                onSeekChanged = onSeekChanged,
                onPrevious = onPrevious,
                onNext = onNext,
                hasNext = hasNext,
                hasPrevious = hasPrevious,
                onSkipOp = onSkipOp,
                onSettings = onSettings,
                videoSettingsEnabled = videoSettingsEnabled
            )
        }
    }
}