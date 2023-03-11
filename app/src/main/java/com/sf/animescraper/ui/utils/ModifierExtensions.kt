package com.sf.animescraper.ui.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.*

fun Modifier.verticalGradientBackground(colors: List<Color>) = this.then(
    drawBehind {
        drawRect(
            // Create a rectangular vertical gradient
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset.Zero,
                end = Offset(0f, Float.POSITIVE_INFINITY)
            ),
            size = size,
        )
    }
)

fun Modifier.secondaryItemAlpha(): Modifier = this.alpha(.78f)

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.clickableNoIndication(
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    this.combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onLongClick = onLongClick,
        onClick = onClick,
    )
}

fun Modifier.clickableTaps(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    onTap: ((Offset)) -> Unit,
    onDoubleTap: ((Offset)) -> Unit,
    onDoubleTapEnd: () -> Unit,
): Modifier = composed {

    val coroutineScope = rememberCoroutineScope()

    var lastDoubleTap by remember {
        mutableStateOf<Long>(0)
    }

    if(lastDoubleTap>0){
        DisposableEffect(lastDoubleTap) {
            val timeout = coroutineScope.launch {
                delay(300)
                onDoubleTapEnd()
            }
            onDispose {
                timeout.cancel()
            }
        }
    }

    pointerInput(interactionSource, enabled) {
        forEachGesture {
            awaitPointerEventScope {
                val firstDown = awaitFirstDown()
                firstDown.consume()
                val firstUp = waitForUpOrCancellation()
                firstUp?.consume()

                if (firstUp != null) {
                    // Check if tap is subsequent tap of double tap
                    if (System.currentTimeMillis() - lastDoubleTap <= 300) {
                        onDoubleTap(firstUp.position)
                        lastDoubleTap = System.currentTimeMillis()
                    } else {
                        try {
                            // Checking for second tap
                            withTimeout(viewConfiguration.doubleTapTimeoutMillis) {
                                val secondDown = awaitFirstDown()
                                secondDown.consume()
                                val secondUp = waitForUpOrCancellation()
                                if (secondUp != null) {
                                    secondUp.consume()
                                    // Second tap found
                                    onDoubleTap(secondDown.position)
                                    lastDoubleTap = System.currentTimeMillis()
                                }
                            }
                        } catch (e: PointerEventTimeoutCancellationException) {
                            // Second tap not found after timeout run single tap callback
                            onTap(firstUp.position)
                        }
                    }
                }
            }
        }
    }
}