package com.sf.tadami.ui.components.filters

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


object TabbedBottomSheetContentPadding {
    val Horizontal = 24.dp
    val Vertical = 8.dp
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TadaBottomSheetLayout(
    sheetContent: @Composable ColumnScope.() -> Unit,
    sheetState: ModalBottomSheetState,
    scrimColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).copy(alpha = 0.4f),
    sheetBackgroundColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
    sheetContentColor: Color = MaterialTheme.colorScheme.contentColorFor(sheetBackgroundColor),
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()


    BackHandler(
        sheetState.isVisible
                || sheetState.targetValue == ModalBottomSheetValue.Expanded
                || sheetState.targetValue == ModalBottomSheetValue.HalfExpanded
    ) {
        coroutineScope.launch {
            sheetState.hide()
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetBackgroundColor = sheetBackgroundColor,
        sheetContentColor = sheetContentColor,
        scrimColor = scrimColor,
        sheetContent = sheetContent,
        content = content
    )
}

