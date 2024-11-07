package com.sf.tadami.ui.components.dialog.sheets

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull

private val mouseSlop = 0.125.dp
private val defaultTouchSlop = 18.dp // The default touch slop on Android devices
private val mouseToTouchSlopRatio = mouseSlop / defaultTouchSlop

interface TadaPointerDirectionConfig {
    fun calculateDeltaChange(offset: Offset): Float
    fun calculatePostSlopOffset(
        totalPositionChange: Offset,
        touchSlop: Float
    ): Offset
}

fun PointerEvent.isPointerUp(pointerId: PointerId): Boolean =
    changes.fastFirstOrNull { it.id == pointerId }?.pressed != true


fun ViewConfiguration.pointerSlop(pointerType: PointerType): Float {
    return when (pointerType) {
        PointerType.Mouse -> touchSlop * mouseToTouchSlopRatio
        else -> touchSlop
    }
}

val TadaBidirectionalPointerDirectionConfig = object : TadaPointerDirectionConfig {
    override fun calculateDeltaChange(offset: Offset): Float = offset.getDistance()

    override fun calculatePostSlopOffset(
        totalPositionChange: Offset,
        touchSlop: Float
    ): Offset {
        val touchSlopOffset =
            totalPositionChange / calculateDeltaChange(totalPositionChange) * touchSlop
        return totalPositionChange - touchSlopOffset
    }
}

suspend inline fun AwaitPointerEventScope.awaitTadaPointerSlopOrCancellation(
    pointerId: PointerId,
    pointerType: PointerType,
    pointerDirectionConfig: TadaPointerDirectionConfig,
    onPointerSlopReached: (PointerInputChange, Offset) -> Unit,
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    val touchSlop = viewConfiguration.pointerSlop(pointerType)
    var pointer: PointerId = pointerId
    var totalPositionChange = Offset.Zero

    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return null
            } else {
                pointer = otherDown.id
            }
        } else {
            val currentPosition = dragEvent.position
            val previousPosition = dragEvent.previousPosition

            val positionChange = currentPosition - previousPosition

            totalPositionChange += positionChange

            val inDirection = pointerDirectionConfig.calculateDeltaChange(
                totalPositionChange
            )

            if (inDirection < touchSlop) {
                // verify that nothing else consumed the drag event
                awaitPointerEvent(PointerEventPass.Final)
            } else {
                val postSlopOffset = pointerDirectionConfig.calculatePostSlopOffset(
                    totalPositionChange,
                    touchSlop
                )

                onPointerSlopReached(
                    dragEvent,
                    postSlopOffset
                )

                return dragEvent

            }
        }
    }
}

suspend fun PointerInputScope.detectTadaDragGestures(
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var drag: PointerInputChange?
        var overSlop = Offset.Zero
        do {
            drag = awaitTadaPointerSlopOrCancellation(
                down.id,
                down.type,
                pointerDirectionConfig = TadaBidirectionalPointerDirectionConfig
            ) { _, over ->
                overSlop = over
            }
        } while (drag != null && overSlop == Offset.Zero)
        if (drag != null) {
            onDragStart.invoke(drag.position)
            onDrag(drag, overSlop)
            if (
                !drag(drag.id) {
                    onDrag(it, it.positionChange())
                }
            ) {
                onDragCancel()
            } else {
                onDragEnd()
            }
        }
    }
}