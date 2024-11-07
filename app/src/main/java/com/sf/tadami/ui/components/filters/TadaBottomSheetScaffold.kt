package com.sf.tadami.ui.components.filters

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


object TabbedBottomSheetContentPadding {
    val Horizontal = 24.dp
    val Vertical = 8.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TadaBottomSheetScaffold(
    sheetContent: @Composable ColumnScope.() -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    showSheet: Boolean,
    onDismissSheet: () -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier,
        topBar,
        bottomBar,
        snackbarHost,
        floatingActionButton,
        floatingActionButtonPosition,
        containerColor,
        contentColor,
        contentWindowInsets,
    ) { paddingValues ->
        content(paddingValues)
        if (showSheet) {
            ModalBottomSheet(
                sheetState = sheetState,
                onDismissRequest = onDismissSheet
            ) {
                sheetContent()
            }
        }
    }
}

