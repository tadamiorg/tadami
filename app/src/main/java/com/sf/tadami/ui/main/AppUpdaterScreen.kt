package com.sf.tadami.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sf.tadami.ui.tabs.more.about.AppUpdateDialog

@Composable
fun AppUpdaterScreen(
    appUpdaterViewModel: AppUpdaterViewModel = viewModel()
) {

    val uiState by appUpdaterViewModel.appUpdaterUiState.collectAsState()

    if (uiState.shouldShowUpdateDialog && uiState.updateInfos?.info != null) {
        AppUpdateDialog(
            onDismissRequest = {
                appUpdaterViewModel.hideDialog()
            },
            uiState = uiState
        )
    }
}