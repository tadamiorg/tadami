package com.sf.tadami.ui.utils

import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastSumBy
import com.sf.tadami.ui.components.widgets.Scroller.STICKY_HEADER_KEY_PREFIX
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.sample

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

fun Modifier.selectedBackground(isSelected: Boolean): Modifier = if (isSelected) {
    composed {
        val alpha = if (isSystemInDarkTheme()) 0.16f else 0.22f
        val color = MaterialTheme.colorScheme.secondary.copy(alpha = alpha)
        Modifier.drawBehind {
            drawRect(color)
        }
    }
} else {
    this
}
fun Modifier.selectedOutline(
    isSelected: Boolean,
    color: Color,
) = this then drawBehind { if (isSelected) drawRect(color = color) }


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

fun Modifier.drawVerticalScrollbar(
    state: LazyListState,
    reverseScrolling: Boolean = false,
    alwaysOn: Boolean = false,
    // The amount of offset the scrollbar position towards the start of the layout
    positionOffsetPx: Float = 0f,
): Modifier = drawScrollbar(state, Orientation.Vertical, reverseScrolling, positionOffsetPx,alwaysOn)

private fun Modifier.drawScrollbar(
    orientation: Orientation,
    reverseScrolling: Boolean,
    alwaysOn : Boolean = false,
    onDraw: ContentDrawScope.(
        reverseDirection: Boolean,
        atEnd: Boolean,
        thickness: Float,
        color: Color,
        alpha: () -> Float,
    ) -> Unit,
): Modifier = composed {
    val scrolled = remember {
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }
    val nestedScrollConnection = remember(orientation, scrolled) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val delta = if (orientation == Orientation.Horizontal) consumed.x else consumed.y
                if (delta != 0f) scrolled.tryEmit(Unit)
                return Offset.Zero
            }
        }
    }

    val alpha = remember { Animatable(0f) }
    LaunchedEffect(scrolled, alpha) {
        scrolled
            .sample(100)
            .collectLatest {
                alpha.snapTo(1f)
                alpha.animateTo(0f, animationSpec = FadeOutAnimationSpec)
            }
    }

    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val reverseDirection = if (orientation == Orientation.Horizontal) {
        if (isLtr) reverseScrolling else !reverseScrolling
    } else {
        reverseScrolling
    }
    val atEnd = if (orientation == Orientation.Vertical) isLtr else true

    val context = LocalContext.current
    val thickness = remember { ViewConfiguration.get(context).scaledScrollBarSize.toFloat() }
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.364f)
    Modifier
        .nestedScroll(nestedScrollConnection)
        .drawWithContent {
            onDraw(reverseDirection, atEnd, thickness, color) { if (alwaysOn) 1f else alpha.value }
        }
}

private fun Modifier.drawScrollbar(
    state: LazyListState,
    orientation: Orientation,
    reverseScrolling: Boolean,
    positionOffset: Float,
    alwaysOn: Boolean
): Modifier = drawScrollbar(
    orientation,
    reverseScrolling,
    alwaysOn
) { reverseDirection, atEnd, thickness, color, alpha ->
    val layoutInfo = state.layoutInfo
    val viewportSize = if (orientation == Orientation.Horizontal) {
        layoutInfo.viewportSize.width
    } else {
        layoutInfo.viewportSize.height
    } - layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding
    val items = layoutInfo.visibleItemsInfo
    val itemsSize = items.fastSumBy { it.size }
    val showScrollbar = items.size < layoutInfo.totalItemsCount || itemsSize > viewportSize
    val estimatedItemSize = if (items.isEmpty()) 0f else itemsSize.toFloat() / items.size
    val totalSize = estimatedItemSize * layoutInfo.totalItemsCount
    val thumbSize = viewportSize / totalSize * viewportSize
    val startOffset = if (items.isEmpty()) {
        0f
    } else {
        items
            .fastFirstOrNull { (it.key as? String)?.startsWith(STICKY_HEADER_KEY_PREFIX)?.not() ?: true }!!
            .run {
                val startPadding = if (reverseDirection) {
                    layoutInfo.afterContentPadding
                } else {
                    layoutInfo.beforeContentPadding
                }
                startPadding + ((estimatedItemSize * index - offset) / totalSize * viewportSize)
            }
    }
    val drawScrollbar = onDrawScrollbar(
        orientation, reverseDirection, atEnd, showScrollbar,
        thickness, color, alpha, thumbSize, startOffset, positionOffset,
    )
    drawContent()
    drawScrollbar()
}

private fun ContentDrawScope.onDrawScrollbar(
    orientation: Orientation,
    reverseDirection: Boolean,
    atEnd: Boolean,
    showScrollbar: Boolean,
    thickness: Float,
    color: Color,
    alpha: () -> Float,
    thumbSize: Float,
    scrollOffset: Float,
    positionOffset: Float,
): DrawScope.() -> Unit {
    val topLeft = if (orientation == Orientation.Horizontal) {
        Offset(
            if (reverseDirection) size.width - scrollOffset - thumbSize else scrollOffset,
            if (atEnd) size.height - positionOffset - thickness else positionOffset,
        )
    } else {
        Offset(
            if (atEnd) size.width - positionOffset - thickness else positionOffset,
            if (reverseDirection) size.height - scrollOffset - thumbSize else scrollOffset,
        )
    }
    val size = if (orientation == Orientation.Horizontal) {
        Size(thumbSize, thickness)
    } else {
        Size(thickness, thumbSize)
    }

    return {
        if (showScrollbar) {
            drawRect(
                color = color,
                topLeft = topLeft,
                size = size,
                alpha = alpha(),
            )
        }
    }
}

private val FadeOutAnimationSpec = tween<Float>(
    durationMillis = ViewConfiguration.getScrollBarFadeDuration(),
    delayMillis = ViewConfiguration.getScrollDefaultDelay(),
)