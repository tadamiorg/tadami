package com.sf.animescraper.ui.utils

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.modifierElementOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

fun Modifier.verticalGradientBackground(from : Color,to : Color) = this.then(
    drawBehind {
        drawRect(
            // Create a vertical gradient between two colors
            brush = Brush.verticalGradient(
                0f to from,
                1f to to,
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

fun Modifier.selectedBackground(isSelected: Boolean): Modifier = composed {
    if (isSelected) {
        val alpha = 0.22f
        background(MaterialTheme.colorScheme.secondary.copy(alpha = alpha))
    } else {
        this
    }
}

fun Modifier.selectedBorderBackground(isSelected: Boolean): Modifier = composed {
    if (isSelected) {
        border(width = 4.dp, shape = RectangleShape, color = MaterialTheme.colorScheme.secondary)
        background(MaterialTheme.colorScheme.secondary)
    } else {
        this
    }
}

@Composable
@ReadOnlyComposable
operator fun PaddingValues.plus(other: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(layoutDirection) +
                other.calculateStartPadding(layoutDirection),
        end = calculateEndPadding(layoutDirection) +
                other.calculateEndPadding(layoutDirection),
        top = calculateTopPadding() + other.calculateTopPadding(),
        bottom = calculateBottomPadding() + other.calculateBottomPadding(),
    )
}