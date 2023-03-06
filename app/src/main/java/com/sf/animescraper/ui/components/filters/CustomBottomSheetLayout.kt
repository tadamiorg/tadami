package com.sf.animescraper.ui.components.filters

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AsBottomSheetLayout(
    sheetContent: @Composable ColumnScope.() -> Unit,
    sheetState: ModalBottomSheetState,
    scrimColor: Color = MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
    onScrimClicked : () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetBackgroundColor = MaterialTheme.colorScheme.background,
        scrimColor = Color.Unspecified,
        sheetContent = sheetContent
    )
    {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            AsScrim(
                color = scrimColor,
                visible = sheetState.targetValue != ModalBottomSheetValue.Hidden,
                onClicked = onScrimClicked
            )
        }
    }
}




