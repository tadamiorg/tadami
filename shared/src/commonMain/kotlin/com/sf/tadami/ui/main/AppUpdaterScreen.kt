package com.sf.tadami.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sf.tadami.navigation.graphs.about.navigateToNewUpdate

/** Opens the new update screen when the launch check finds a newer release. */
@Composable
fun AppUpdaterScreen(
    navController: NavController,
    appUpdaterViewModel: AppUpdaterViewModel = viewModel()
) {

    val newRelease by appUpdaterViewModel.newRelease.collectAsState()

    LaunchedEffect(newRelease) {
        newRelease?.let { release ->
            navController.navigateToNewUpdate(release)
            appUpdaterViewModel.onNewReleaseShown()
        }
    }
}
