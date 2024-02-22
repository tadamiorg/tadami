package com.sf.tadami.ui.animeinfos.episode.player.controls.youtube.shapes

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

val RightShape = GenericShape { size, _ ->
    val cornerRadius = size.width * 0.5f // adjust the radius to your preference
    val rightRect = Rect(
        offset = Offset(0f, 0f - size.height * 0.1f),
        size = Size(size.width * 0.5f, size.height * 1.2f)
    )

    moveTo(cornerRadius, 0f)

    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(cornerRadius, size.height)


    arcTo(
        rect = rightRect,
        startAngleDegrees = 90f,
        sweepAngleDegrees = 180f,
        forceMoveTo = false
    )
}