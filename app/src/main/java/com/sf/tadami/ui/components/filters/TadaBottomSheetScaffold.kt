package com.sf.tadami.ui.components.filters

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sf.tadami.ui.components.material.FabPosition
import com.sf.tadami.ui.components.material.Scaffold


object TabbedBottomSheetContentPadding {
    val Horizontal = 24.dp
    val Vertical = 8.dp
}

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
    // ModalBottomSheet
    onDismissRequest: () -> Unit,
    sheetModifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    sheetShape: Shape = BottomSheetDefaults.ExpandedShape,
    sheetContainerColor: Color = BottomSheetDefaults.ContainerColor,
    sheetContentColor: Color = contentColorFor(containerColor),
    sheetTonalElevation: Dp = 0.dp,
    sheetScrimColor: Color = BottomSheetDefaults.ScrimColor,
    sheetDragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    sheetContentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    sheetProperties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    sheetContent: @Composable ColumnScope.() -> Unit,
    // Scaffold Content
    content: @Composable (PaddingValues) -> Unit
) {
    if (showSheet) {
        ModalBottomSheet(
            modifier = sheetModifier,
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            sheetMaxWidth = sheetMaxWidth,
            sheetGesturesEnabled = sheetGesturesEnabled,
            shape = sheetShape,
            containerColor = sheetContainerColor,
            contentColor = sheetContentColor,
            tonalElevation = sheetTonalElevation,
            scrimColor = sheetScrimColor,
            dragHandle = sheetDragHandle,
            contentWindowInsets = sheetContentWindowInsets,
            properties = sheetProperties,
        ) {
            sheetContent()
        }
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

