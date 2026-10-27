package com.sf.tadami.ui.components.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.sf.tadami.ui.utils.drawVerticalScrollbar

@Composable
fun ScrollbarLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    userScrollEnabled: Boolean = true,
    scrollBarAlwaysOn : Boolean = false,
    content: LazyListScope.() -> Unit,
) {
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current
    val positionOffset = remember(contentPadding) {
        with(density) { contentPadding.calculateEndPadding(direction).toPx() }
    }
    LazyColumn(
        modifier = modifier
            .drawVerticalScrollbar(
                state = state,
                reverseScrolling = reverseLayout,
                positionOffsetPx = positionOffset,
                alwaysOn = scrollBarAlwaysOn
            ),
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        userScrollEnabled = userScrollEnabled,
        content = content,
    )
}