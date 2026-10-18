package com.sf.tadami.ui.tabs.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sf.tadami.ui.category.visualName
import com.sf.tadami.ui.components.screens.TabText
import kotlinx.coroutines.launch

/** The category tabs of the library, swipeable, with the tab row hidden when there is only one */
@Composable
fun LibraryPager(
    modifier: Modifier = Modifier,
    tabs: List<LibraryTab>,
    activeCategoryId: Long,
    onActiveCategoryChanged: (categoryId: Long) -> Unit,
    badgeCount: (LibraryTab) -> Int?,
    pageContent: @Composable (LibraryTab) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val initialPage = remember { tabs.indexOfFirst { it.category.id == activeCategoryId }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage) { tabs.size }

    val currentTabs by rememberUpdatedState(tabs)
    val currentActiveCategoryId by rememberUpdatedState(activeCategoryId)
    val currentOnActiveCategoryChanged by rememberUpdatedState(onActiveCategoryChanged)

    // A category added, deleted or moved shifts the tabs: back on the active one, or the nearest when it is
    // gone. Then the tab shown becomes the active one
    LaunchedEffect(pagerState, tabs.map { it.category.id }) {
        val activeIndex = currentTabs.indexOfFirst { it.category.id == currentActiveCategoryId }
        when {
            activeIndex >= 0 && activeIndex != pagerState.currentPage -> pagerState.scrollToPage(activeIndex)
            activeIndex < 0 && pagerState.currentPage > currentTabs.lastIndex -> {
                pagerState.scrollToPage(currentTabs.lastIndex)
            }
        }
        snapshotFlow { pagerState.settledPage }.collect { page ->
            currentTabs.getOrNull(page)?.let { currentOnActiveCategoryChanged(it.category.id) }
        }
    }

    Column(modifier = modifier) {
        if (tabs.showTabRow()) {
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage.coerceIn(0, tabs.lastIndex),
                modifier = Modifier.zIndex(1f),
                edgePadding = 0.dp,
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { TabText(text = tab.category.visualName(), badgeCount = badgeCount(tab)) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            verticalAlignment = Alignment.Top,
            key = { page -> currentTabs.getOrNull(page)?.category?.id ?: -page - 1L },
        ) { page ->
            currentTabs.getOrNull(page)?.let { pageContent(it) }
        }
    }
}
