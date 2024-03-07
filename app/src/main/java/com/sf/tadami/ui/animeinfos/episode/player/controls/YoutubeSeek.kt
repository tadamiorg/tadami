package com.sf.tadami.ui.animeinfos.episode.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import com.sf.tadami.R
import com.sf.tadami.ui.animeinfos.episode.player.controls.youtube.SeekAnimation
import com.sf.tadami.ui.animeinfos.episode.player.controls.youtube.shapes.LeftShape
import com.sf.tadami.ui.animeinfos.episode.player.controls.youtube.shapes.RightShape
import com.sf.tadami.ui.animeinfos.episode.player.controls.youtube.youtubeDetectTapGestures
import com.sf.tadami.ui.utils.padding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GetYoutubeGesture(
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSimpleTap: () -> Unit,
    onSeekingChange : (Boolean) -> Unit,
    lockedControls : Boolean,
    playerSeekValue: Long,
) {
    val preferencesSeekValue by remember(playerSeekValue){
        derivedStateOf{
            playerSeekValue/1000
        }
    }
    val coroutineScope = rememberCoroutineScope()
    var isSeeking by remember { mutableStateOf(false) }
    var timerJob: Job? by remember { mutableStateOf(null) }

    val rightInteractionSource = remember { MutableInteractionSource() }
    val leftInteractionSource = remember { MutableInteractionSource() }

    var rightRipple: Indication? by remember { mutableStateOf(null) }
    var leftRipple: Indication? by remember { mutableStateOf(null) }

    val leftRippleEffect = rememberRipple(bounded = true, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    val rightRippleEffect = rememberRipple(bounded = true,color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

    var backwardSeekedTime by remember {  mutableLongStateOf(0L) }
    var forwardSeekedTime by remember {  mutableLongStateOf(0L) }
    var seekedTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(seekedTime) {
        if (seekedTime > 0) forwardSeekedTime = seekedTime
        if (seekedTime < 0) backwardSeekedTime = seekedTime
    }

    LaunchedEffect(rightRipple) {
        if (rightRipple == null) return@LaunchedEffect
        launch {
            val press = PressInteraction.Press(Offset.Zero)
            rightInteractionSource.emit(press)
            rightInteractionSource.emit(PressInteraction.Release(press))
        }
    }

    LaunchedEffect(leftRipple) {
        if (leftRipple == null) return@LaunchedEffect
        launch {
            val press = PressInteraction.Press(Offset.Zero)
            leftInteractionSource.emit(press)
            leftInteractionSource.emit(PressInteraction.Release(press))
        }
    }

    fun resetTap() {
        timerJob?.cancel() // Cancel the previous job if it exists
        timerJob = null
        timerJob = coroutineScope.launch {
            delay(500)
            isSeeking = false
            rightRipple = null
            leftRipple = null
            onSeekingChange(false)
        }
    }

    suspend fun PressGestureScope.pressInteract(source: MutableInteractionSource, offset: Offset) {
        val press = PressInteraction.Press(offset)
        source.emit(press)
        tryAwaitRelease()
        source.emit(PressInteraction.Release(press))
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraLarge)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(LeftShape)
                .pointerInput(leftInteractionSource,preferencesSeekValue) {
                    if(!lockedControls){
                        youtubeDetectTapGestures(
                            onPress = { offset: Offset ->
                                pressInteract(leftInteractionSource, offset)
                            },
                            onDoubleTap = {
                                onSeekBackward()
                                seekedTime = -preferencesSeekValue
                                isSeeking = true
                                resetTap()
                                leftRipple = leftRippleEffect
                                onSeekingChange(true)
                            },
                            onTap = {
                                if (isSeeking) {
                                    onSeekBackward()
                                    if (leftRipple == null) leftRipple = leftRippleEffect
                                    if (seekedTime > 0) seekedTime = -preferencesSeekValue else seekedTime -= preferencesSeekValue
                                    resetTap()
                                } else {
                                    onSimpleTap()
                                }
                            },
                            disabledDoubleTap = { isSeeking }
                        )
                    }

                }
                .pointerInput(leftInteractionSource){
                    detectDragGestures { _, _ ->  }
                }
                .indication(
                    indication = leftRipple,
                    interactionSource = leftInteractionSource,
                ),
            color = Color.Transparent
        ) {
            AnimatedVisibility(
                visible = isSeeking && seekedTime < 0,
                exit = if (isSeeking) ExitTransition.None else fadeOut() + shrinkHorizontally()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                )
                {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        SeekAnimation(reversed = true)
                        Text(text = stringResource(id = R.string.youtube_gestures_seconds,backwardSeekedTime))
                    }
                }
            }
        }
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(RightShape)
                .pointerInput(rightInteractionSource,preferencesSeekValue) {
                    if(!lockedControls){
                        youtubeDetectTapGestures(
                            onPress = { offset: Offset ->
                                pressInteract(rightInteractionSource, offset)
                            },
                            onDoubleTap = {
                                onSeekForward()
                                seekedTime = preferencesSeekValue
                                isSeeking = true
                                resetTap()
                                rightRipple = rightRippleEffect
                                onSeekingChange(true)
                            },
                            onTap = {
                                if (isSeeking) {
                                    onSeekForward()
                                    if (rightRipple == null) rightRipple = rightRippleEffect
                                    if (seekedTime < 0) seekedTime =
                                        preferencesSeekValue else seekedTime += preferencesSeekValue
                                    resetTap()
                                } else {
                                    onSimpleTap()
                                }
                            },
                            disabledDoubleTap = { isSeeking }
                        )
                    }
                }
                .pointerInput(rightInteractionSource){
                    detectDragGestures { _, _ ->  }
                }
                .indication(
                    indication = rightRipple,
                    interactionSource = rightInteractionSource,
                ),
            color = Color.Transparent

        ) {
            AnimatedVisibility(
                visible = isSeeking && seekedTime > 0,
                exit = if (isSeeking) ExitTransition.None else fadeOut() + shrinkHorizontally()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        SeekAnimation()
                        Text(text = stringResource(id = R.string.youtube_gestures_seconds,forwardSeekedTime))
                    }
                }
            }
        }
    }
}










