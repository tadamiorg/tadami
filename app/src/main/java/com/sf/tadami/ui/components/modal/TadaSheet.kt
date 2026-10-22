package com.sf.tadami.ui.components.modal

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** The colour of the sheets: surfaceContainerLow, surfaceContainerHigh on pure black (Low is a black band there). */
@Composable
@ReadOnlyComposable
fun sheetContainerColor(): Color =
    if (isPureBlack()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow

/**
 * The bottom sheet of the screens' options: Material 3, drag handle, 28dp top corners, fully expanded,
 * at most 640dp wide, on phones and tablets alike. A 1dp outline on pure black.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TadaSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val container = sheetContainerColor()
    val shape = BottomSheetDefaults.ExpandedShape
    // The sheet's window keeps the bar icons of the screen from its first frame
    val bars = screenBarsAppearance()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.then(modalBorder()?.let { Modifier.border(it, shape) } ?: Modifier),
        sheetState = sheetState,
        sheetMaxWidth = ModalDefaults.SheetMaxWidth,
        shape = shape,
        containerColor = container,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        properties = ModalBottomSheetProperties(
            isAppearanceLightStatusBars = bars.lightStatusBars,
            isAppearanceLightNavigationBars = bars.lightNavigationBars,
        ),
    ) {
        // The sheet draws behind the navigation bar, with no system background or scrim on it
        TransparentDialogBars()
        content()
    }
}

/** A page of a sheet. */
class SheetTab(val title: String, val content: @Composable () -> Unit)

/**
 * The pages of a sheet under a tab row of the sheet's colour, no tab row for a single page. The pager
 * takes the height of the page shown.
 */
@Composable
fun SheetTabs(
    tabs: List<SheetTab>,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
) {
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    Column {
        if (ModalRules.showsTabRow(tabs.size)) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage, containerColor = sheetContainerColor()) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.animateContentSize(),
        ) { page ->
            Box(modifier = Modifier.padding(contentPadding)) {
                tabs[page].content()
            }
        }
    }
}
