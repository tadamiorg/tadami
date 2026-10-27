package com.sf.tadami.ui.components.filters

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sf.tadami.ui.components.material.FabPosition
import com.sf.tadami.ui.components.material.Scaffold
import com.sf.tadami.ui.components.modal.TadaSheet


/** A screen with its options in a [TadaSheet], shown while [showSheet]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TadaBottomSheetScaffold(
    // Scafffold
    modifier: Modifier = Modifier,
    showSheet: Boolean,
    topBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState(),
    ),
    topBar: @Composable (TopAppBarScrollBehavior) -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    // Sheet
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    sheetContent: @Composable ColumnScope.() -> Unit,
    // Scaffold Content
    content: @Composable (PaddingValues) -> Unit
) {
    if (showSheet) {
        TadaSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            content = sheetContent,
        )
    }

    Scaffold(
        modifier = modifier,
        topBarScrollBehavior = topBarScrollBehavior,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
    ) { paddingValues ->
        content(paddingValues)
    }
}
