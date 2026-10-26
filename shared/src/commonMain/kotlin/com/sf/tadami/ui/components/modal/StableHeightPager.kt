package com.sf.tadami.ui.components.modal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity

/**
 * The pages of a modal, as tall as the tallest one so switching pages doesn't resize the modal: a
 * shorter page leaves room at its bottom. Every page is laid out to know its height; the modal's own
 * height limit still caps it, where a page's list scrolls.
 */
@Composable
fun StableHeightPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    pageContent: @Composable (page: Int) -> Unit,
) {
    // Each page's own height, whatever the pager's
    val heights = remember { mutableStateMapOf<Int, Int>() }
    val tallest = with(LocalDensity.current) { ModalRules.tallestPage(heights, state.pageCount).toDp() }
    HorizontalPager(
        state = state,
        verticalAlignment = Alignment.Top,
        beyondViewportPageCount = (state.pageCount - 1).coerceAtLeast(0),
        modifier = modifier.heightIn(min = tallest),
    ) { page ->
        Box(
            modifier = Modifier
                // Not stretched to the pager's height, the page's own is measured
                .wrapContentHeight(Alignment.Top)
                .onSizeChanged { heights[page] = it.height },
        ) {
            pageContent(page)
        }
    }
}
