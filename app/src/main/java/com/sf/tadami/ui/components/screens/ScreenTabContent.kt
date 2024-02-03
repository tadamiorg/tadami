package com.sf.tadami.ui.components.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.sf.tadami.ui.components.data.Action
data class ScreenTabContent(
    @StringRes val titleRes : Int,
    val actions : List<Action> = emptyList(),
    val badgeNumber: Int? = null,
    val searchEnabled: Boolean = false,
    val content : @Composable (contentPadding: PaddingValues, snackbarHostState: SnackbarHostState) -> Unit,
)

