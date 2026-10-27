package com.sf.tadami.ui.components.material

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpSize
import kotlin.math.abs

/**
 * A wheel of [texts], [rowCount] rows in [size], that snaps to the row in its middle. [onScrollFinished]
 * gets the index of that row when the wheel stops, and returns the index to scroll to instead, or null.
 * As WheelPickerCompose's, which is Android only.
 */
@Composable
fun WheelTextPicker(
    texts: List<String>,
    rowCount: Int,
    size: DpSize,
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = LocalContentColor.current,
    onScrollFinished: (snappedIndex: Int) -> Int? = { null },
) {
    val state = rememberLazyListState(startIndex)
    val rowHeight = size.height / rowCount

    LaunchedEffect(state.isScrollInProgress, texts.size) {
        if (!state.isScrollInProgress) {
            onScrollFinished(state.middleIndex() ?: startIndex)?.let { state.scrollToItem(it) }
        }
    }

    LazyColumn(
        modifier = modifier.size(size),
        state = state,
        contentPadding = PaddingValues(vertical = rowHeight * ((rowCount - 1) / 2)),
        flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Center),
    ) {
        items(texts.size) { index ->
            Box(
                modifier = Modifier
                    .height(rowHeight)
                    .width(size.width)
                    .graphicsLayer {
                        // Fading and tilting away from the middle, a row away at most
                        val distance = state.distanceToMiddle(index)
                        if (distance != null) {
                            alpha = if (abs(distance) <= 1f) 1.2f - abs(distance) else 0.2f
                            rotationX = -20f * distance
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = texts[index], style = style, color = color, maxLines = 1)
            }
        }
    }
}

private fun LazyListState.viewportMiddle(): Int =
    (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2

/** The row nearest to the middle of the wheel. */
private fun LazyListState.middleIndex(): Int? {
    val middle = viewportMiddle()
    return layoutInfo.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - middle) }?.index
}

/** How far the row [index] is from the middle of the wheel, in rows, below it when positive. */
private fun LazyListState.distanceToMiddle(index: Int): Float? {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return null
    if (item.size == 0) return null
    return (item.offset + item.size / 2 - viewportMiddle()).toFloat() / item.size
}
