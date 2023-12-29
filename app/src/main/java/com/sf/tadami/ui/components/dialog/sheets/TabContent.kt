package com.sf.tadami.ui.components.dialog.sheets

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

data class TabContent(
    @StringRes val titleRes: Int,
    val content: @Composable () -> Unit
)