package com.sf.tadami.ui.components.dialog.sheets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.surfaceColorAtElevation
import com.sf.tadami.ui.components.filters.TabbedBottomSheetContentPadding
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class
)
@Composable
fun TabbedBottomSheet(
    tabs: List<TabContent>,
    beyondBoundsPageCount: Int = (tabs.size - 1).coerceAtLeast(0),
    sheetState: ModalBottomSheetState
) {
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { tabs.size })

    Column(modifier = Modifier.preventBottomSheetJumps(sheetState)) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    modifier = Modifier.zIndex(6f),
                    text = { Text(text = stringResource(id = tab.titleRes)) },
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        val density = LocalDensity.current
        var largestHeight by rememberSaveable { mutableStateOf(0f) }

        HorizontalPager(
            modifier = Modifier.heightIn(min = largestHeight.dp),
            state = pagerState,
            verticalAlignment = Alignment.Top,
            beyondBoundsPageCount = beyondBoundsPageCount,
        ) { page ->
            Box(
                modifier = Modifier
                    .padding(vertical = TabbedBottomSheetContentPadding.Vertical, horizontal = TabbedBottomSheetContentPadding.Horizontal)
                    .onSizeChanged {
                        with(density) {
                            val heightDp = it.height.toDp()
                            if (heightDp.value > largestHeight) {
                                largestHeight = heightDp.value
                            }
                        }
                    },
            ) {
                tabs[page].content()
            }
        }
    }


}