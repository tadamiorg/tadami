package com.sf.tadami.ui.components.dialog.simple

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.sf.tadami.ui.components.dialog.alert.AlertDialogConstants
import com.sf.tadami.ui.components.screens.ScreenTabContent
import com.sf.tadami.ui.components.screens.TabText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabbedSimpleDialog(
    modifier: Modifier = Modifier,
    tabs: List<ScreenTabContent>,
    onDismissRequest: () -> Unit,
    opened: Boolean,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    val state = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    SimpleDialog(
        opened = opened,
        onDismissRequest = onDismissRequest,
        dialogHorizontalPadding = false,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    ) {

        Column(
            modifier = modifier,
        ) {
            PrimaryTabRow(
                selectedTabIndex = state.currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.zIndex(1f),
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = state.currentPage == index,
                        onClick = { scope.launch { state.animateScrollToPage(index) } },
                        text = {
                            TabText(
                                text = stringResource(tab.titleRes),
                                badgeCount = tab.badgeNumber
                            )
                        },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = state,
                verticalAlignment = Alignment.Top,
            ) { page ->
                tabs[page].content(
                    AlertDialogConstants.DialogHorizontalPadding,
                    snackbarHostState
                )
            }
        }

    }
}