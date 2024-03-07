package com.sf.tadami.ui.tabs.updates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.sf.tadami.R
import com.sf.tadami.navigation.graphs.app.animeInfos.AnimeInfosRoutes
import com.sf.tadami.ui.components.bottombar.ContextualBottomBar
import com.sf.tadami.ui.components.data.Action
import com.sf.tadami.ui.components.topappbar.ContextualTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    setNavDisplay: (display: Boolean) -> Unit,
    bottomNavDisplay: Boolean,
    updatesViewModel: UpdatesViewModel = viewModel()
) {
    val uiState by updatesViewModel.uiState.collectAsState()
    val isRefreshing by updatesViewModel.isRefreshing.collectAsState()

    val snackBarHostState = remember { SnackbarHostState() }


    LaunchedEffect(uiState.selectionMode){
        if (!uiState.selectionMode) {
            setNavDisplay(true)
        }else{
            setNavDisplay(false)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ContextualTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.label_recent_updates_long)
                    )
                },
                actions = listOf(
                    Action.Drawable(
                        title = R.string.stub_text,
                        R.drawable.ic_refresh,
                        onClick = {
                            updatesViewModel.updateLibrary()
                        }
                    )
                ),
                actionModeCounter = uiState.selected.size,
                onCloseActionModeClicked = { updatesViewModel.toggleAllSelectedItems(false) },
                onToggleAll = { updatesViewModel.toggleAllSelectedItems(true) },
                onInverseAll = { updatesViewModel.inverseSelectedItems() }
            )
        },
        bottomBar = {
            ContextualBottomBar(
                visible = uiState.selected.isNotEmpty() && !bottomNavDisplay,
                actions = listOf(
                    Action.Vector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.DoneAll,
                        onClick = {
                            updatesViewModel.setUpdatesSeenStatus()
                        },
                    ),
                    Action.Vector(
                        title = R.string.stub_text,
                        icon = Icons.Outlined.RemoveDone,
                        onClick = {
                            updatesViewModel.setUpdatesUnSeenStatus()
                        },
                    ),
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            UpdatesComponent(
                uiState = uiState,
                onUpdateLibrary = updatesViewModel::updateLibrary,
                isRefreshing = isRefreshing,
                onClickCover = {
                    navController.navigate("${AnimeInfosRoutes.DETAILS}/${it.update.sourceId}/${it.update.animeId}")
                },
                onUpdateSelected = updatesViewModel::toggleSelectedItems,
                onOpenEpisode = {
                    navController.navigate("${AnimeInfosRoutes.EPISODE}/${it.update.sourceId}/${it.update.episodeId}")
                },
                snackBarHostState = snackBarHostState
            )
        }
    }

    DisposableEffect(Unit) {
        updatesViewModel.resetNewUpdatesCount()

        onDispose {
            updatesViewModel.resetNewUpdatesCount()
        }
    }
}